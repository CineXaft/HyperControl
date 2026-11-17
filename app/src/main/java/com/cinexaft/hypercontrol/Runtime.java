package com.cinexaft.hypercontrol;

import android.app.Application;
import android.content.Context;
import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import com.cinexaft.hypercontrol.securitycenter.SecurityCenter;
import java.io.File;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class Runtime {

    /** Packages this module acts on; anything else returns immediately. */
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String POWER_KEEPER = "com.miui.powerkeeper";
    private static final String HTML_VIEWER = "com.android.htmlviewer";
    private static final String GUARD_PROVIDER = "com.miui.guardprovider";
    private static final String DOWNLOADS = "com.android.providers.downloads";
    private static final String XMSF = "com.xiaomi.xmsf";
    private static final String SECURITY_CENTER = "com.miui.securitycenter";

    private static boolean installed;

    /** Package this process was loaded for, and its class loader; kept for {@link #ready}. */
    private static String pkg;
    private static ClassLoader loader;

    /**
     * Work that needs a live Context, run once from the Application.attach hook.
     *
     * <p>Both DEX-backed features read their switches here, so they are only wired up when they
     * are already on: changing one therefore needs a restart of the host process, like every other
     * per-process feature in this module.
     */
    public static void ready(Context context) {
        if (loader == null) {
            return;
        }
        if (XMSF.equals(pkg) && State.enabled(13)) {
            XmsAuth.install(context, loader);
        } else if (SECURITY_CENTER.equals(pkg)) {
            SecurityCenter.install(context, loader);
        }
    }

    private static boolean handled(String packageName) {
        return SYSTEMUI.equals(packageName)
                || POWER_KEEPER.equals(packageName)
                || HTML_VIEWER.equals(packageName)
                || GUARD_PROVIDER.equals(packageName)
                || DOWNLOADS.equals(packageName)
                || XMSF.equals(packageName)
                || SECURITY_CENTER.equals(packageName);
    }

    private static void downloads(ClassLoader classLoader) {
        hook(classLoader, "com.android.providers.downloads.config.XLConfig", "setDebug",
                new Class[]{Context.class, Boolean.TYPE, String.class}, 6, Void.TYPE);
        hook(classLoader, "com.android.providers.downloads.config.XLConfig", "setSoDebug",
                new Class[]{Boolean.TYPE, String.class}, 6, Void.TYPE);
        hook(classLoader, "com.android.providers.downloads.util.FileUtil", "createFile",
                new Class[]{String.class}, 7, File.class);
    }

    /**
     * Hooks one target if it still has the expected signature. A firmware that changed the return
     * type simply skips this hook instead of installing one that would throw on every call.
     */
    private static void hook(ClassLoader classLoader, String className, String name,
                             Class<?>[] params, int feature, Class<?> returnType) {
        Method method = Hooks.bySignature(classLoader, className, name, params, returnType);
        if (method == null) {
            Log.w(Hooks.TAG, "signature changed, skipped: " + className + "#" + name);
            return;
        }
        FeatureHook hooker = new FeatureHook(feature);
        if (feature == 7) {
            Reflect.attach(method, hooker, XposedInterface.ExceptionMode.PASSTHROUGH);
        } else {
            Reflect.hook(method, hooker);
        }
    }

    public static synchronized void install(String packageName, ClassLoader classLoader) {
        Log.i(Hooks.TAG, packageName);
        if (!handled(packageName) || installed) {
            return;
        }
        installed = true;
        pkg = packageName;
        loader = classLoader;
        try {
            Reflect.hook(Application.class.getDeclaredMethod("attach", Context.class), new FeatureHook(-1));
        } catch (Throwable error) {
            Reflect.log(error);
        }
        try {
            if (SYSTEMUI.equals(packageName)) {
                SignalHook.install(classLoader);
                MobileWifiHook.install(classLoader);
                SwapHook.install(classLoader);
                ClockExpand.install(classLoader);
                hook(classLoader, "com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaControllerImpl$mediaDataChangeListener$1",
                        "onMediaDataChanged",
                        new Class[]{classLoader.loadClass("com.android.systemui.media.controls.shared.model.MediaData")}, 0, Void.TYPE);
                hook(classLoader, "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl",
                        "getMobileTypeName", new Class[]{Integer.TYPE}, 10, String.class);
            } else if (POWER_KEEPER.equals(packageName)) {
                hook(classLoader, "com.miui.powerkeeper.cloudcontrol.LocalUpdateUtils", "startCloudSyncData",
                        new Class[]{Context.class, Boolean.TYPE}, 4, Void.TYPE);
                hook(classLoader, "miui.process.ProcessManager", "kill",
                        new Class[]{classLoader.loadClass("miui.process.ProcessConfig")}, 3, Boolean.TYPE);
            } else if (HTML_VIEWER.equals(packageName)) {
                hook(classLoader, "com.android.settings.cloud.JobTask", "updateCloudAllData",
                        new Class[0], 5, Void.TYPE);
            } else if (DOWNLOADS.equals(packageName)) {
                downloads(classLoader);
            }
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }
}
