package com.cinexaft.hypercontrol.base;

import android.app.Activity; import android.app.ActivityThread; import android.content.Context; import android.content.res.Resources; import android.os.Bundle; import android.text.TextUtils;
import android.util.Log; import com.cinexaft.hypercontrol.base.Entry.Targets; import com.cinexaft.hypercontrol.base.PageBridge.Strings; import com.cinexaft.hypercontrol.hook.systemframework.core.R; import dalvik.system.PathClassLoader; import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Executable; import java.lang.reflect.Field; import java.lang.reflect.Method; import java.util.List;

public final class SettingsHook {
    private static final String MODULE_PACKAGE = "com.cinexaft.hypercontrol";
    private static final String ENTRY_KEY = "hypercontrol_entry", ENTRY_TAG = "HyperControl.Entry";
    static final String SCREEN = "com.cinexaft.hypercontrol.base.NativeScreen";
    private static Method bridge;
    private static ClassLoader host;
    private static String apk;
    private static Class<?> screen;
    private SettingsHook() { }

    public static void install(ClassLoader loader) throws ReflectiveOperationException {
        Method headers = loader.loadClass("com.android.settings.MiuiSettings").getDeclaredMethod("updateHeaderList", List.class);
        Method switchPage = loader.loadClass("com.android.settings.SettingsActivity").getDeclaredMethod("switchToFragment",
                String.class, Bundle.class, boolean.class, int.class, CharSequence.class);
        Method loadPage = loader.loadClass("androidx.fragment.app.FragmentFactory")
                .getDeclaredMethod("loadFragmentClass", ClassLoader.class, String.class);
        host = loader;
        // Navigation must be installed before publishing the homepage entry.
        XposedInterface.HookHandle loadHook = null, switchHook = null;
        try {
            loadHook = Reflect.hook(loadPage, chain -> SCREEN.equals(chain.getArg(1)) ? loadScreen() : chain.proceed());
            switchHook = Reflect.hook(switchPage, chain -> {
                if (!SCREEN.equals(chain.getArg(0))) return chain.proceed();
                loadScreen();
                bridge.invoke(null, chain.getThisObject(), chain.getArg(1), chain.getArg(4));
                return null;
            });
            Reflect.hook(headers, SettingsHook::updateHeaders);
        } catch (Throwable error) {
            rollback(switchHook, error);
            rollback(loadHook, error);
            throw error;
        }
    }
    private static void rollback(XposedInterface.HookHandle handle, Throwable error) {
        if (handle == null) return;
        try { handle.unhook(); }
        catch (Throwable failure) { if (failure != error) error.addSuppressed(failure); }
    }
    private static void append(Context context, List<?> headers) throws Throwable {
        Resources resources = context.getResources();
        int myDeviceId = resources.getIdentifier("my_device", "id", Targets.SETTINGS);
        Object anchor = null;
        int anchorIndex = -1;
        for (int index = 0; index < headers.size(); index++) {
            Object header = headers.get(index);
            if (TextUtils.equals(ENTRY_KEY, (CharSequence) R.get(header, "key"))) return;
            if (anchor == null && ((myDeviceId != 0 && (int) R.findField(header.getClass(), "id").getLong(header) == myDeviceId)
                    || "com.android.settings.device.MiuiMyDeviceSettings".equals(R.get(header, "fragment")))) {
                anchor = header;
                anchorIndex = index;
            }
        }
        if (anchor == null) { Log.w(ENTRY_TAG, "My device header not found; skip insertion"); return; }
        insertAfter(context, headers, anchorIndex, anchor, resources);
    }
    private static void insertAfter(Context context, List<?> headers, int index, Object anchor, Resources resources) throws Throwable {
        int iconId = resources.getIdentifier("dynamic_item_icon", "drawable", Targets.SETTINGS);
        if (iconId == 0) {
            Field iconField = host.loadClass("com.android.settings.R$drawable").getDeclaredField("dynamic_item_icon");
            iconField.setAccessible(true);
            iconId = iconField.getInt(null);
            if (iconId == 0) { Log.w(ENTRY_TAG, "dynamic_item_icon not found; skip insertion"); return; }
        }
        apk = context.createPackageContext(MODULE_PACKAGE, 0).getApplicationInfo().sourceDir;
        loadScreen(); // Do not publish an entry whose bridge or Fragment cannot be resolved.
        Class<?> type = anchor.getClass();
        Object entry = type.getDeclaredConstructor().newInstance();
        Field groupId = R.findField(type, "groupId");
        groupId.setInt(entry, groupId.getInt(anchor));
        R.findField(type, "iconRes").setInt(entry, iconId);
        R.set(entry, "key", ENTRY_KEY);
        String label = Strings.appLabel(context, MODULE_PACKAGE);
        R.set(entry, "title", label);
        R.set(entry, "breadCrumbTitle", label);
        R.set(entry, "fragment", SCREEN);
        Bundle arguments = new Bundle();
        arguments.putString("title", label);
        R.set(entry, "fragmentArguments", arguments);
        R.set(entry, "extras", new Bundle());
        R.findField(type, "id").setLong(entry, 1213809409L);
        @SuppressWarnings("unchecked") List<Object> mutableHeaders = (List<Object>) headers;
        mutableHeaders.add(index + 1, entry);
    }
    private static synchronized Class<?> loadScreen() throws Throwable {
        if (screen != null) return screen;
        if (apk == null) apk = ActivityThread.currentApplication().getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0).sourceDir;
        // Both UI classes resolve Settings and AndroidX through the same host parent.
        PathClassLoader loader = new PathClassLoader(apk, host);
        Method show = loader.loadClass("com.cinexaft.hypercontrol.base.PageBridge")
                .getMethod("show", Activity.class, Bundle.class, CharSequence.class);
        Class<?> resolved = loader.loadClass(SCREEN);
        bridge = show;
        screen = resolved;
        return screen;
    }
    private static Object updateHeaders(XposedInterface.Chain chain) throws Throwable {
        Object result = chain.proceed();
        try {
            if (chain.getThisObject() instanceof Context context && chain.getArg(0) instanceof List<?> headers) append(context, headers);
        } catch (Throwable error) { Reflect.log(error); }
        return result;
    }
    public static final class Reflect {
        private Reflect() { }
        public static XposedInterface.HookHandle attach(Executable target, XposedInterface.Hooker hooker, XposedInterface.ExceptionMode mode) {
            return Entry.module.hook(target).setExceptionMode(mode).intercept(hooker);
        }
        public static XposedInterface.HookHandle hook(Method method, XposedInterface.Hooker hooker) {
            return attach(method, hooker, XposedInterface.ExceptionMode.PROTECTIVE);
        }
        public static void log(Throwable error) { Log.e("HyperControl", "Hook error", error); }
    }
}
