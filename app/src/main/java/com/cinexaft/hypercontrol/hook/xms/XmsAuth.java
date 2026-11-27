package com.cinexaft.hypercontrol.hook.xms;

import android.os.Bundle; import android.util.Log; import com.cinexaft.hypercontrol.base.Entry; import com.cinexaft.hypercontrol.base.Entry.Hooks; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect;
import java.lang.reflect.Method; import org.luckypray.dexkit.DexKitBridge; import org.luckypray.dexkit.query.FindClass; import org.luckypray.dexkit.query.matchers.ClassMatcher; import org.luckypray.dexkit.result.ClassData; import org.luckypray.dexkit.result.MethodData;

/** XMS focus-auth hook. */
public final class XmsAuth {
    private static final String TAG = "HyperControl.XMS";
    private static final String SUCCESS = "Auth is successful";
    private static final String BUNDLE = Bundle.class.getName();
    private static boolean installed;
    private XmsAuth() { }
    public static synchronized void install(ClassLoader loader) {
        if (installed) return;
        try {
            Entry.loadDexKitLibrary();
            try (DexKitBridge dex = DexKitBridge.create(loader, false)) {
                for (ClassData session : dex.findClass(new FindClass()
                        .matcher(new ClassMatcher().usingStrings(SUCCESS)))) {
                    Method success = find(session, loader, 0);
                    Method failure = find(session, loader, 1);
                    if (success == null || failure == null) continue;
                    if (!Hooks.install(failure, chain -> {
                        if (!State.enabled(13)) return chain.proceed();
                        Object error = chain.getArg(0);
                        if (error == null || error instanceof Bundle || error instanceof CharSequence) {
                            return chain.proceed();
                        }
                        try {
                            return success.invoke(chain.getThisObject());
                        } catch (ReflectiveOperationException errorWhileBuildingSuccess) {
                            Reflect.log(errorWhileBuildingSuccess);
                            return chain.proceed();
                        }
                    })) continue;
                    installed = true;
                    Log.i(TAG, "focus-auth result hook installed: " + failure);
                    return;
                }
                Log.w(TAG, "auth session not found; feature inactive");
            }
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }
    private static Method find(ClassData owner, ClassLoader loader, int parameterCount)
            throws ReflectiveOperationException {
        Method found = null;
        for (MethodData data : owner.getMethods()) {
            if (!BUNDLE.equals(data.getReturnTypeName())
                    || data.getParamTypes().size() != parameterCount) continue;
            if (found != null) return null;
            found = data.getMethodInstance(loader);
        }
        return found;
    }
}
