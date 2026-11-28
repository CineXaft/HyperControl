package com.cinexaft.hypercontrol.base;

import android.app.Application; import android.content.ContentResolver; import android.content.Context; import android.provider.Settings; import android.text.TextUtils;
import android.util.Log; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import com.cinexaft.hypercontrol.hook.systemframework.core.Engine; import com.cinexaft.hypercontrol.hook.systemframework.SafeVolume; import com.cinexaft.hypercontrol.hook.systemframework.PasskeyHooks; import com.cinexaft.hypercontrol.hook.systemframework.FlagSecure;
import com.cinexaft.hypercontrol.hook.downloads.DownloadHooks; import com.cinexaft.hypercontrol.hook.html.HtmlHooks; import com.cinexaft.hypercontrol.hook.power.PowerHooks; import com.cinexaft.hypercontrol.hook.systemui.SystemUiHooks; import com.cinexaft.hypercontrol.hook.xms.XmsAuth; import com.cinexaft.hypercontrol.hook.wechat.WechatHooks;
import io.github.libxposed.api.XposedInterface; import io.github.libxposed.api.XposedModule; import io.github.libxposed.api.XposedModuleInterface;
import java.io.File; import java.lang.reflect.Method; import static com.cinexaft.hypercontrol.base.Entry.Targets.*;

public final class Entry extends XposedModule {
    public static Entry module;
    private static boolean installed, dexKitLoaded;


    // A failure affects only this installation, not the next independent feature.
    private interface Action { void run() throws Throwable; }
    private static boolean attempt(String tag, String message, Action action) {
        try { action.run(); return true; }
        catch (Throwable error) { Log.e(tag, message, error); return false; }
    }
    public static synchronized void loadDexKitLibrary() {
        if (dexKitLoaded) return;
        String directory = module.getModuleApplicationInfo().nativeLibraryDir;
        if (TextUtils.isEmpty(directory)) throw new IllegalStateException("无法获取模块 native library 目录");
        File library = new File(directory, System.mapLibraryName("dexkit"));
        if (!library.isFile()) throw new IllegalStateException("DexKit native library 不存在：" + library.getAbsolutePath());
        System.load(library.getAbsolutePath());
        dexKitLoaded = true;
    }
    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        module = this;
        WechatHooks.onModuleLoaded(param.getProcessName());
    }
    @Override
    public synchronized void onPackageLoaded(XposedModuleInterface.PackageLoadedParam param) {
        module = this;
        String pkg = param.getPackageName();
        ClassLoader loader = param.getDefaultClassLoader();
        if (!SETTINGS.equals(pkg)) attempt(Hooks.TAG, "App hooks: " + pkg, () -> AppHooks.install(pkg, loader));
        else if (!installed) installed = attempt(Hooks.TAG, "Settings hook initialization failed", () -> SettingsHook.install(loader));
    }
    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        module = this;
        WechatHooks.onPackageReady(this, param);
        attempt("HyperControl.Passkey", "Passkey package hooks unavailable", () ->
                PasskeyHooks.packages(this, param.getPackageName(), param.getClassLoader(), param.getApplicationInfo()));
    }
    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        module = this;
        ClassLoader loader = param.getClassLoader();
        attempt("HyperControl.Passkey", "Passkey system hooks unavailable", () -> PasskeyHooks.server(this, loader));
        Log.i("HyperControl.Core", "ENTRY system_server");
        attempt("HyperControl.Core", "ENTRY core installation aborted", () -> Engine.install(loader));
        attempt(Hooks.TAG, "FlagSecure installation failed", () -> FlagSecure.install(loader));
        attempt("HyperControl.Volume", "ENTRY volume installation aborted", () -> SafeVolume.install(loader));
    }

    public static final class AppHooks {
        private static boolean attachInstalled, featuresAttempted;
        private AppHooks() { }
        public static synchronized void install(String pkg, ClassLoader loader) {
            if (!Targets.isAppPage(pkg) || WECHAT.equals(pkg)) return;
            if (!attachInstalled) attachInstalled = attempt(Hooks.TAG, "Application attach hook: " + pkg, () ->
                Reflect.hook(Application.class.getDeclaredMethod("attach", Context.class), chain -> {
                    Object result = chain.proceed();
                    if (chain.getArg(0) instanceof Context context && pkg.equals(context.getPackageName())) {
                        State.bind(context, pkg);
                        if (XMS.equals(pkg) && State.enabled(13)) {
                            Object host = chain.getThisObject();
                            XmsAuth.install(host instanceof Application app ? app.getClassLoader() : context.getClassLoader());
                        }
                    }
                    return result;
                }));
            if (featuresAttempted) return;
            featuresAttempted = true;
            attempt(Hooks.TAG, "App feature installation: " + pkg, () -> {
                switch (pkg) {
                    case SYSTEM_UI -> SystemUiHooks.install(loader);
                    case POWER -> PowerHooks.install(loader);
                    case HTML -> HtmlHooks.install(loader);
                    case DOWNLOADS -> DownloadHooks.install(loader);
                    default -> { }
                }
            });
        }
    }
    public static final class Bindings {
        private Bindings() { }
        public static void hook(ClassLoader loader, String owner, String name, Class<?> result,
                                XposedInterface.Hooker hooker, Class<?>... params) {
            hook(loader, owner, name, result, XposedInterface.ExceptionMode.PROTECTIVE, hooker, params);
        }
        public static void hook(ClassLoader loader, String owner, String name, Class<?> result,
                                XposedInterface.ExceptionMode mode, XposedInterface.Hooker hooker, Class<?>... params) {
            Method method = Hooks.bySignature(loader, owner, name, params, result);
            if (method != null) Reflect.attach(method, hooker, mode);
            else Log.w(Hooks.TAG, "signature changed, skipped: " + owner + "#" + name);
        }
        public static XposedInterface.Hooker override(int feature, Object value) {
            return chain -> State.enabled(feature) ? value : chain.proceed();
        }
    }
    public static final class Hooks {
        public static final String TAG = "HyperControl";
        private Hooks() { }
        // Match name and arity only; callers needing overload safety use bySignature.
        public static Method byNameCount(Class<?> owner, String name, int count) {
            if (owner == null) return null;
            for (Method method : owner.getDeclaredMethods()) {
                if (name.equals(method.getName()) && method.getParameterTypes().length == count) {
                    method.setAccessible(true);
                    return method;
                }
            }
            return null;
        }
        public static Method bySignature(ClassLoader loader, String owner, String name, Class<?>[] params, Class<?> result) {
            try {
                Method method = loader.loadClass(owner).getDeclaredMethod(name, params);
                if (method.getReturnType() != result) return null;
                method.setAccessible(true);
                return method;
            } catch (Throwable ignored) { return null; }
        }
        public static boolean install(Method method, XposedInterface.Hooker hooker) { return install(method, hooker, TAG); }
        public static boolean install(Method method, XposedInterface.Hooker hooker, String tag) {
            if (method == null) return false;
            try {
                if (Entry.module != null) Entry.module.deoptimize(method);
                Reflect.hook(method, hooker);
                Log.i(tag, method.toString());
                return true;
            } catch (Throwable error) { Reflect.log(error); return false; }
        }
    }
    public static final class State {
        public static final String MOBILE_TEXT = "hypercontrol_mobile_type_text";
        // Stable feature slots; null entries must not be removed or renumbered.
        private static final String[] KEYS = {
            "hide_media_island", null, null, "power_dont_kill", "power_disable_cloud", // 0–4
            "html_disable_cloud", "downloads_remove_xl", "hide_mobile_activity", // 5–7
            "hide_wifi_activity", "hide_wifi_standard", "hide_mobile_type_on_wifi", null, // 8–11
            "swap_mobile_wifi", "xms_auth_unlock", null, null, null, // 12–16
            "wechat_pad", "wechat_auto_select_original", null, // 17–19
            "wechat_disable_typing_status", "wechat_hide_sns_ads", "lockscreen_clock_no_collapse" // 20–22
        };
        private static volatile boolean[] flags;
        private static volatile String mobileText;
        public static void bind(Context context, String pkg) {
            int[] features = switch (pkg) {
                case SYSTEM_UI -> new int[]{0, 7, 8, 9, 10, 12, 22};
                case POWER -> new int[]{3, 4};
                case HTML -> new int[]{5};
                case DOWNLOADS -> new int[]{6};
                case XMS -> new int[]{13};
                case WECHAT -> new int[]{17, 18, 20, 21};
                default -> null;
            };
            if (features == null) return;
            boolean[] loaded = new boolean[KEYS.length];
            ContentResolver resolver = context.getContentResolver();
            for (int feature : features) {
                try { loaded[feature] = Settings.Global.getInt(resolver, setting(KEYS[feature]), 0) == 1; }
                catch (Throwable error) { Reflect.log(error); }
            }
            if (SYSTEM_UI.equals(pkg)) {
                mobileText = null;
                try { mobileText = Settings.Global.getString(resolver, MOBILE_TEXT); }
                catch (Throwable error) { Reflect.log(error); }
            }
            flags = loaded;
        }
        public static String customMobileText() {
            String text = mobileText;
            return TextUtils.isEmpty(text) ? null : text;
        }
        public static boolean enabled(int index) {
            boolean[] current = flags;
            return current != null && current[index];
        }
        public static String key(int index) {
            String key = KEYS[index];
            if (key == null) throw new IllegalArgumentException("No setting for slot " + index);
            return key;
        }
        public static String setting(String name) { return "hypercontrol_".concat(name); }
        public static int signalMask() {
            boolean[] current = flags;
            return current == null ? 0 : (current[7] ? 1 : 0) | (current[8] ? 2 : 0) | (current[9] ? 4 : 0);
        }
    }
    public static final class Keys {
        private static final String[] names = "downgrade|bypass_digest|bypass_exact_sig_match|bypass_shared_user|bypass_verification|use_previous_signatures|disable_verification_agent|bypass_block|allow_hidden_apis_for_system_apps|bypass_resource_arsc_restrictions".split("\\|");
        public static String key(int index) { return "system_framework_n_".concat(name(index)); }
        public static String name(int index) { return names[index]; }
    }
    public static final class Targets {
        public static final String FRAMEWORK = "android";
        public static final String SETTINGS = "com.android.settings";
        public static final String SYSTEM_UI = "com.android.systemui";
        public static final String POWER = "com.miui.powerkeeper";
        public static final String HTML = "com.android.htmlviewer";
        public static final String DOWNLOADS = "com.android.providers.downloads";
        public static final String XMS = "com.xiaomi.xmsf";
        public static final String WECHAT = "com.tencent.mm";
        static boolean isAppPage(String target) {
            return target != null && switch (target) {
                case SYSTEM_UI, POWER, HTML, DOWNLOADS, XMS, WECHAT -> true;
                default -> false;
            };
        }
        static boolean canRestart(String target) { return FRAMEWORK.equals(target) || isAppPage(target); }
        private Targets() { }
    }
}
