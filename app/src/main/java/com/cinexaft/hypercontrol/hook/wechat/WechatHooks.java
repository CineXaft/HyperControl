package com.cinexaft.hypercontrol.hook.wechat;

import android.app.ActivityThread;
import android.content.Context;
import android.util.Log;
import com.cinexaft.hypercontrol.base.Entry;
import com.cinexaft.hypercontrol.base.Entry.State;
import com.cinexaft.hypercontrol.base.Entry.Targets;
import io.github.libxposed.api.XposedModuleInterface;
import org.luckypray.dexkit.DexKitBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Coordinates WeChat-specific hook setup. */
public final class WechatHooks {
    private static final String TAG = "HyperControl.WeChat";
    private static volatile boolean mainProcess;

    private interface Action { void run() throws Throwable; }

    private WechatHooks() { }

    public static void onModuleLoaded(String processName) {
        mainProcess = Targets.WECHAT.equals(processName);
    }

    public static void onPackageReady(Entry module, XposedModuleInterface.PackageReadyParam param) {
        if (!mainProcess || !Targets.WECHAT.equals(param.getPackageName()) || !param.isFirstPackage()) return;
        try {
            Context context = wechatContext();
            State.bind(context, Targets.WECHAT);
            ClassLoader loader = param.getClassLoader();
            if (State.enabled(17)) {
                try {
                    Entry.loadDexKitLibrary();
                    try (DexKitBridge dex = DexKitBridge.create(param.getApplicationInfo().sourceDir)) {
                        new WechatPad(module).install(dex, loader);
                    }
                } catch (Throwable error) {
                    module.log(Log.ERROR, "TabletHook", "com.tencent.mm 平板登录 Hook 初始化失败", error);
                }
            }
            installFeatures(loader);
        } catch (Throwable error) {
            Log.e(TAG, "WeChat hooks initialization failed", error);
        }
    }

    private static Context wechatContext() throws Exception {
        ActivityThread thread = ActivityThread.currentActivityThread();
        Context packageContext = thread.getSystemContext().createPackageContext(Targets.WECHAT, 0);
        Class<?> contextImpl = Class.forName("android.app.ContextImpl");
        Field packageInfo = contextImpl.getDeclaredField("mPackageInfo");
        packageInfo.setAccessible(true);
        Method createAppContext = contextImpl.getDeclaredMethod(
                "createAppContext", ActivityThread.class, packageInfo.getType());
        createAppContext.setAccessible(true);
        Context context = (Context) createAppContext.invoke(null, thread, packageInfo.get(packageContext));
        String attributionPackage = context.getAttributionSource().getPackageName();
        if (!Targets.WECHAT.equals(attributionPackage)) {
            throw new IllegalStateException("Unexpected settings attribution: " + attributionPackage);
        }
        return context;
    }

    private static void installFeatures(ClassLoader loader) {
        if (State.enabled(18)) {
            attempt("Original-image setup failed", () -> WechatOriginalImage.install(loader));
        }
        if (!State.enabled(20) && !State.enabled(21)) return;
        try {
            Entry.loadDexKitLibrary();
            try (DexKitBridge dex = DexKitBridge.create(loader, false)) {
                if (State.enabled(20)) attempt("Typing setup failed", () -> WechatTyping.install(dex, loader));
                if (State.enabled(21)) attempt("Ad-filter setup failed", () -> WechatAds.install(dex, loader));
            }
        } catch (Throwable error) {
            Log.e(TAG, "WeChat DexKit hooks initialization failed", error);
        }
    }

    private static void attempt(String message, Action action) {
        try { action.run(); }
        catch (Throwable error) { Log.e(TAG, message, error); }
    }
}
