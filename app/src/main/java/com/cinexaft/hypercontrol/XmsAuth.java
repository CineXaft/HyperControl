package com.cinexaft.hypercontrol;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

/**
 * Forces XMS authentication to report success instead of an error.
 *
 * <p>XMS builds two result bundles for one session: the success builder ({@code ()Bundle}) and the
 * error builder ({@code (AuthError)Bundle}). Replacing the error result with the success one is
 * enough — nothing downstream reads the error afterwards, so the error object is only inspected for
 * the log line and never modified.
 *
 * <p>Class and method names are gone on a release ROM (the auth module ships as {@code a…n} after
 * R8), so the target is found by structure through {@link Dex}: the class that contains the
 * literal {@code "Auth is successful"} — a string the success builder must log — and inside it the
 * zero-argument and one-argument methods that both return a {@link Bundle}. A firmware that
 * renames or rewrites either builder makes the search empty, which disables this switch instead of
 * hooking something unrelated.
 */
public final class XmsAuth implements XposedInterface.Hooker {

    /** Literal from the success builder; strings survive R8 even when identifiers do not. */
    private static final String SUCCESS = "Auth is successful";
    private static final String BUNDLE = "android.os.Bundle";

    /** {@code ()Bundle}: builds the "auth is successful" result. */
    private static Method success;
    /** {@code (AuthError)Bundle}: dispatches the failure. */
    private static Method failure;

    private static boolean installed;

    public static void install(Context context, ClassLoader loader) {
        if (installed) {
            return;
        }
        DexKitBridge bridge = Dex.of(context, loader);
        if (bridge == null) {
            return;
        }
        try {
            ClassMatcher matcher = new ClassMatcher().usingStrings(SUCCESS);
            for (ClassData candidate : bridge.findClass(new FindClass().matcher(matcher))) {
                if (bind(candidate, loader)) {
                    installed = true;
                    Hooks.install(failure, new XmsAuth());
                    return;
                }
            }
            Log.w(Hooks.TAG, "no auth session found, unlock switch inactive");
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }

    /** Picks the two bundle builders out of one candidate class. */
    private static boolean bind(ClassData candidate, ClassLoader loader) {
        MethodData ok = null;
        MethodData bad = null;
        for (MethodData method : candidate.getMethods()) {
            if (!BUNDLE.equals(method.getReturnTypeName())) {
                continue;
            }
            int params = method.getParamTypes().size();
            if (params == 0) {
                ok = method;
            } else if (params == 1) {
                bad = method;
            }
        }
        if (ok == null || bad == null) {
            return false;
        }
        try {
            success = ok.getMethodInstance(loader);
            failure = bad.getMethodInstance(loader);
            Log.i(Hooks.TAG, "auth session " + candidate.getName() + ": " + failure + " -> " + success);
            return true;
        } catch (Throwable error) {
            Reflect.log(error);
            return false;
        }
    }

    /** Reads the error code for the log line; the object itself is never modified. */
    private static int code(Object error) {
        try {
            for (Field field : error.getClass().getDeclaredFields()) {
                if (field.getType() == int.class && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return field.getInt(error);
                }
            }
        } catch (Throwable ignored) {
            // A field we cannot read only costs the log line.
        }
        return -1;
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (success == null || !State.enabled(13)) {
            return chain.proceed();
        }
        Object error = chain.getArg(0);
        if (error == null || error instanceof Bundle || error instanceof CharSequence) {
            return chain.proceed();
        }
        Object result;
        try {
            // The success builder reports through the same callback the failure would have used,
            // so the caller still sees exactly one result.
            result = success.invoke(chain.getThisObject());
        } catch (Throwable failure) {
            // A session that failed before it was usable has nothing to report success about;
            // keep the original error instead of turning it into a crash.
            Reflect.log(failure);
            return chain.proceed();
        }
        Log.i(Hooks.TAG, "auth forced to success, error " + code(error) + " dropped");
        return result;
    }
}
