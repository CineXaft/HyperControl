package com.cinexaft.hypercontrol.wechatpad;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import com.cinexaft.hypercontrol.Entry;
import com.cinexaft.hypercontrol.State;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Identifies WeChat as Lenovo TB-9707F using structural discovery from the supplied wxpad APK. */
public final class WechatPad {
    private static final String TAG = "HyperControl.WechatPad";
    private static volatile boolean installed;

    private WechatPad() {}

    public static synchronized void install(Context context, ClassLoader loader) {
        if (installed || !State.enabled(17)) return;
        installed = true;
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo("com.tencent.mm", 0);
            List<Target> targets = DexScanner.scan(apkPaths(info));
            boolean[] hooked = new boolean[2];
            for (Target target : targets) {
                if (target.kind >= 0 && target.kind < 2 && !hooked[target.kind]
                        && hookOne(loader, target)) hooked[target.kind] = true;
            }
            for (int kind = 0; kind < hooked.length; kind++) {
                if (!hooked[kind]) Log.w(TAG, "no compatible hook found for kind=" + kind);
            }
        } catch (Throwable error) {
            Log.e(TAG, "WeChat pad hook setup failed", error);
        }
    }

    private static String[] apkPaths(ApplicationInfo info) {
        if (info == null) return new String[0];
        ArrayList<String> paths = new ArrayList<>();
        if (info.sourceDir != null) paths.add(info.sourceDir);
        if (info.splitSourceDirs != null) {
            for (String path : info.splitSourceDirs) if (path != null && !paths.contains(path)) paths.add(path);
        }
        return paths.toArray(new String[0]);
    }

    private static boolean hookOne(ClassLoader loader, Target target) {
        try {
            Method method = resolve(loader, target);
            if (method == null) return false;
            XposedInterface.Hooker hooker = target.kind == 0
                    ? chain -> Boolean.TRUE
                    : chain -> {
                        Object result = chain.proceed();
                        return Boolean.TRUE.equals(result) || result == null || result instanceof Boolean
                                ? Boolean.TRUE : result;
                    };
            Entry.module.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(hooker);
            Log.i(TAG, "hooked kind=" + target.kind + " " + method);
            return true;
        } catch (Throwable error) {
            Log.w(TAG, "hook rejected: " + target, error);
            return false;
        }
    }

    private static Method resolve(ClassLoader loader, Target target) {
        try {
            for (Method method : Class.forName(target.className, false, loader).getDeclaredMethods()) {
                if (!target.methodName.equals(method.getName())) continue;
                Class<?>[] params = method.getParameterTypes();
                if (target.kind == 0) {
                    if (Modifier.isStatic(method.getModifiers()) && params.length == 0
                            && method.getReturnType() == boolean.class) return method;
                } else if (!Modifier.isStatic(method.getModifiers()) && params.length >= 2
                        && params[0] == String.class && params[1] == String.class
                        && params[params.length - 1].getName().endsWith("Continuation")
                        && method.getReturnType() == Object.class) return method;
            }
        } catch (Throwable ignored) {}
        return null;
    }
}
