package dev.local.hypercontrol;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import java.io.File;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class Runtime {
    private static boolean installed;

    private static void downloads(ClassLoader classLoader) {
        Class[] clsArr = {Context.class, Boolean.TYPE, String.class};
        Class cls = Void.TYPE;
        hook(classLoader, "com.android.providers.downloads.config.XLConfig", "setDebug", clsArr, 6, cls);
        hook(classLoader, "com.android.providers.downloads.config.XLConfig", "setSoDebug", new Class[]{Boolean.TYPE, String.class}, 6, cls);
        hook(classLoader, "com.android.providers.downloads.util.FileUtil", "createFile", new Class[]{String.class}, 7, File.class);
    }

    private static void hook(ClassLoader classLoader, String str, String str2, Class[] clsArr, int i, Class cls) {
        try {
            Method declaredMethod = classLoader.loadClass(str).getDeclaredMethod(str2, clsArr);
            if (declaredMethod.getReturnType() != cls) {
                throw new IllegalStateException("Hook target return type changed");
            }
            FeatureHook featureHook = new FeatureHook(i);
            if (i != 7) {
                Reflect.hook(declaredMethod, featureHook);
            } else {
                Reflect.attach(declaredMethod, featureHook, XposedInterface.ExceptionMode.PASSTHROUGH);
            }
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    public static synchronized void install(String str, ClassLoader classLoader) {
        Log.i("HyperControl", str);
        boolean zEquals = "com.android.systemui".equals(str);
        boolean zEquals2 = "com.miui.powerkeeper".equals(str);
        boolean zEquals3 = "com.android.htmlviewer".equals(str);
        if ((!(zEquals | zEquals2 | zEquals3 | "com.miui.guardprovider".equals(str)) && !"com.android.providers.downloads".equals(str)) || installed) {
            return;
        }
        installed = true;
        try {
            Reflect.hook(Application.class.getDeclaredMethod("attach", Context.class), new FeatureHook(-1));
            if (zEquals) {
                try {
                    SignalHook.install(classLoader);
                    MobileWifiHook.install(classLoader);
                    ClockExpand.install(classLoader);
                } catch (Throwable th) {
                    Reflect.log(th);
                }
                hook(classLoader, "com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaControllerImpl$mediaDataChangeListener$1", "onMediaDataChanged", new Class[]{classLoader.loadClass("com.android.systemui.media.controls.shared.model.MediaData")}, 0, Void.TYPE);
                hook(classLoader, "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl", "getMobileTypeName", new Class[]{Integer.TYPE}, 10, String.class);
                return;
            }
            if (zEquals2) {
                hook(classLoader, "com.miui.powerkeeper.cloudcontrol.LocalUpdateUtils", "startCloudSyncData", new Class[]{Context.class, Boolean.TYPE}, 4, Void.TYPE);
                hook(classLoader, "miui.process.ProcessManager", "kill", new Class[]{classLoader.loadClass("miui.process.ProcessConfig")}, 3, Boolean.TYPE);
            } else if (zEquals3) {
                hook(classLoader, "com.android.settings.cloud.JobTask", "updateCloudAllData", new Class[0], 5, Void.TYPE);
            } else if ("com.android.providers.downloads".equals(str)) {
                downloads(classLoader);
            }
        } catch (Throwable th2) {
            Reflect.log(th2);
        }
    }
}
