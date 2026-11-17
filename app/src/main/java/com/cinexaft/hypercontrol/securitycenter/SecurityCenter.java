package com.cinexaft.hypercontrol.securitycenter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.cinexaft.hypercontrol.Dex;
import com.cinexaft.hypercontrol.Hooks;
import com.cinexaft.hypercontrol.Reflect;
import com.cinexaft.hypercontrol.State;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

/**
 * The three Security Center features, located structurally instead of by name.
 *
 * <p>Security Center ships as a 61 MB APK with three dex files, and its internals are renamed by
 * every release: the previous implementation pinned {@code ScoreManager.q()/o()} and a
 * {@code Log.d} scrape, so it only worked on the firmware it was written for. Everything here is
 * found through {@link Dex} instead, and each lookup degrades to "feature off" with a log line
 * rather than hooking something unrelated.
 *
 * <ul>
 *   <li><b>Score</b> — the method that logs {@code getMinusPredictScore} is the deduction
 *       calculator. Returning 0 leaves the final score at 100.
 *   <li><b>Interception</b> — every {@code protected ()int} method in the intercept fragments is
 *       a countdown in seconds before the dialog may be confirmed. Returning 0 enables the button
 *       immediately. The layout id also happens to be an {@code int}, but it is {@code public},
 *       so the {@code protected} filter keeps it out.
 *   <li><b>ADB verification</b> — {@code AdbInstallVerifyActivity} itself has an early exit for a
 *       specific device and region; this takes the same exit unconditionally, so the account and
 *       network round trip never starts.
 * </ul>
 */
public final class SecurityCenter {

    /** Debug literal from the deduction calculator; the one string that survives R8. */
    private static final String SCORE_TAG = "getMinusPredictScore";
    /** Simple class name fragment of the intercept fragment hierarchy. */
    private static final String INTERCEPT = "InterceptBaseFragment";
    /** Simple class name fragment of the ADB install verification activity. */
    private static final String ADB = "AdbInstallVerifyActivity";

    private static boolean installed;

    private SecurityCenter() {
    }

    /**
     * Installs whatever is enabled. Called once per Security Center process, after the switches
     * have been read — the DEX index is only built when at least one of them is on.
     */
    public static void install(Context context, ClassLoader loader) {
        if (installed) {
            return;
        }
        installed = true;
        boolean score = State.enabled(14);
        boolean intercept = State.enabled(15);
        boolean adb = State.enabled(16);
        if (!score && !intercept && !adb) {
            return;
        }
        DexKitBridge bridge = Dex.of(context, loader);
        if (bridge == null) {
            return;
        }
        if (score) {
            score(bridge, loader);
        }
        if (intercept) {
            intercept(bridge, loader);
        }
        if (adb) {
            adb(bridge, loader);
        }
    }

    /** Zeroes the predicted deduction, which leaves the reported score at 100. */
    private static void score(DexKitBridge bridge, ClassLoader loader) {
        try {
            MethodData data = bridge.findMethod(new FindMethod()
                    .matcher(new MethodMatcher().usingStrings(SCORE_TAG))).firstOrNull();
            if (data == null) {
                Log.w(Hooks.TAG, "no score calculator, switch inactive");
                return;
            }
            if (!"int".equals(data.getReturnTypeName()) || !data.getParamTypes().isEmpty()) {
                Log.w(Hooks.TAG, "score calculator changed shape: " + data.getName());
                return;
            }
            Hooks.install(data.getMethodInstance(loader), new Zero());
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }

    /** Cuts the confirmation countdown of the permission intercept dialogs to zero. */
    private static void intercept(DexKitBridge bridge, ClassLoader loader) {
        try {
            ClassMatcher base = new ClassMatcher().className(INTERCEPT, StringMatchType.Contains, false);
            Set<ClassData> found = new LinkedHashSet<>(
                    bridge.findClass(new FindClass().matcher(base)));
            // The countdown is overridden in the concrete fragments, so their copies need hooking
            // as well even though the base declares it.
            found.addAll(bridge.findClass(new FindClass()
                    .matcher(new ClassMatcher().superClass(base))));
            int count = 0;
            for (ClassData data : found) {
                // The name filter also matches the anonymous classes inside the fragments
                // (InterceptBaseFragment$a/b); only the fragments themselves are targets.
                if (data.getName().indexOf('$') >= 0) {
                    continue;
                }
                Class<?> type;
                try {
                    type = data.getInstance(loader);
                } catch (Throwable error) {
                    Reflect.log(error);
                    continue;
                }
                for (Method method : type.getDeclaredMethods()) {
                    int modifiers = method.getModifiers();
                    if (method.getReturnType() != int.class || method.getParameterTypes().length != 0
                            || !Modifier.isProtected(modifiers) || Modifier.isAbstract(modifiers)
                            || Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    Hooks.install(method, new Zero());
                    count++;
                }
            }
            if (count == 0) {
                Log.w(Hooks.TAG, "no intercept countdown, switch inactive");
            }
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }

    /**
     * Ends the ADB install verification activity before it can ask anything.
     *
     * <p>The superclass {@code onCreate} is invoked first so the activity initialises normally,
     * then it finishes — the same state the ROM's own early-exit branch produces.
     */
    private static void adb(DexKitBridge bridge, ClassLoader loader) {
        try {
            for (ClassData data : bridge.findClass(new FindClass()
                    .matcher(new ClassMatcher().className(ADB, StringMatchType.Contains, false)))) {
                if (data.getName().indexOf('$') >= 0) {
                    continue;
                }
                Class<?> type = data.getInstance(loader);
                Method onCreate;
                try {
                    onCreate = type.getDeclaredMethod("onCreate", Bundle.class);
                } catch (NoSuchMethodException missing) {
                    continue;
                }
                Hooks.install(onCreate, new Skip(superOnCreate(type)));
                return;
            }
            Log.w(Hooks.TAG, "no adb verify activity, switch inactive");
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }

    /** Walks up to the first class that declares {@code onCreate(Bundle)}. */
    private static Method superOnCreate(Class<?> type) {
        for (Class<?> parent = type.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
            try {
                Method method = parent.getDeclaredMethod("onCreate", Bundle.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Keep walking; Activity itself is the last candidate.
            }
        }
        return null;
    }

    /** Returns {@code 0} for a method whose caller only cares about the number. */
    private static final class Zero implements XposedInterface.Hooker {
        public Object intercept(XposedInterface.Chain chain) {
            return 0;
        }
    }

    /** Runs the parent {@code onCreate}, finishes the activity and skips the original body. */
    private static final class Skip implements XposedInterface.Hooker {
        private final Method parent;

        Skip(Method parent) {
            this.parent = parent;
        }

        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            Object self = chain.getThisObject();
            if (parent == null || !(self instanceof Activity)) {
                return chain.proceed();
            }
            if (!State.enabled(16)) {
                return chain.proceed();
            }
            try {
                parent.invoke(self, chain.getArg(0));
                ((Activity) self).finish();
                Log.i(Hooks.TAG, "adb install verification skipped");
                return null;
            } catch (Throwable error) {
                // Anything unexpected here means the normal verification flow stays in place.
                Reflect.log(error);
                return chain.proceed();
            }
        }
    }
}
