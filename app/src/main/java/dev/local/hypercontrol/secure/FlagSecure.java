package dev.local.hypercontrol.secure;

import android.util.Log;
import dev.local.hypercontrol.Reflect;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class FlagSecure {
    public static volatile boolean enabled;
    private static boolean installed;

    private static void attach(Method method) {
        if (method.getReturnType() != Boolean.TYPE) {
            throw new IllegalArgumentException("FlagSecure target must return boolean");
        }
        method.setAccessible(true);
        Reflect.hook(method, new Callback());
        Log.i("HyperControl.FlagSecure", method.toString());
    }

    public static synchronized void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        installed = true;
        try {
            Method declaredMethod = classLoader.loadClass("com.android.server.SystemServer").getDeclaredMethod("startOtherServices", classLoader.loadClass("com.android.server.utils.TimingsTraceAndSlog"));
            declaredMethod.setAccessible(true);
            Reflect.hook(declaredMethod, new BootHook());
        } catch (Throwable th) {
            Reflect.log(th);
        }
        try {
            attach(classLoader.loadClass("com.android.server.wm.WindowState").getDeclaredMethod("isSecureLocked", new Class[0]));
        } catch (Throwable th2) {
            Reflect.log(th2);
        }
        try {
            attach(classLoader.loadClass("com.android.server.wm.WindowManagerServiceImpl").getDeclaredMethod("notAllowCaptureDisplay", classLoader.loadClass("com.android.server.wm.RootWindowContainer"), Integer.TYPE));
        } catch (Throwable th3) {
            Reflect.log(th3);
        }
    }
}
