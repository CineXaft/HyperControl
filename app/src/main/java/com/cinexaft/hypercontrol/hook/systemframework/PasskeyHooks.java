package com.cinexaft.hypercontrol.hook.systemframework;

import android.annotation.SuppressLint; import android.app.ActivityThread; import android.content.ComponentName; import android.content.Context; import android.content.pm.ActivityInfo; import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager; import android.credentials.CredentialManager; import android.os.Build; import android.provider.Settings; import android.util.Log; import android.view.View;
import android.widget.CompoundButton; import com.cinexaft.hypercontrol.base.Entry; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import com.cinexaft.hypercontrol.hook.systemframework.core.Config; import com.cinexaft.hypercontrol.hook.systemframework.core.R;
import io.github.libxposed.api.XposedInterface; import io.github.libxposed.api.XposedModule; import java.lang.reflect.Constructor; import java.lang.reflect.Executable; import java.lang.reflect.Field; import java.lang.reflect.Method;
import java.lang.reflect.Modifier; import java.util.Collection; import java.util.ArrayList; import java.util.HashSet; import java.util.List; import java.util.Set; import java.util.concurrent.ConcurrentHashMap; import org.luckypray.dexkit.DexKitCacheBridge;
import org.luckypray.dexkit.query.FindMethod; import org.luckypray.dexkit.query.matchers.MethodMatcher; import org.luckypray.dexkit.result.ClassData; import org.luckypray.dexkit.wrap.DexMethod; import sun.misc.Unsafe;

/** Passkey hooks for system_server, Settings, Security Center and Scanner. */
@SuppressLint({"PrivateApi", "BlockedPrivateApi", "SoonBlockedPrivateApi"})
public final class PasskeyHooks {
    private static final String TAG = "HyperControl.Passkey";
    private static final String SETTING = State.setting("system_framework_passkey");
    private static final String SETTINGS = "com.android.settings";
    private static final String SECURITY_CENTER = "com.miui.securitycenter";
    private static final String CREDENTIALS = "com.android.settings.applications.credentials";
    private static final String CONTROLLER =
            "Lcom/android/settings/applications/credentials/CredentialManagerPreferenceController;->";
    private static final ComponentName GOOGLE_CHOOSER = new ComponentName("com.google.android.gms",
            "com.google.android.gms.identitycredentials.ui.CredentialChooserActivity");
    private static final Set<Executable> installed = new HashSet<>();
    private static boolean dexKitInitialized;

    private PasskeyHooks() { }

    public static synchronized void packages(XposedModule module, String packageName,
            ClassLoader loader, ApplicationInfo app) {
        try {
            switch (packageName) {
                case SETTINGS -> installSettings(module, loader, app);
                case SECURITY_CENTER -> installSecurityCenter(module, loader, app);
                case "com.xiaomi.scanner" -> installScanner(module, loader);
                default -> { }
            }
        } catch (Throwable error) {
            Log.w(TAG, "Package hook setup failed: " + packageName, error);
        }
    }
    public static synchronized void server(XposedModule module, ClassLoader loader) {
        installCredentialSessions(module, loader);
        installChooser(module, loader);
    }
    private static void installSettings(XposedModule module, ClassLoader loader, ApplicationInfo app) {
        Field international = internationalFlag(loader);
        XposedInterface.Hooker regionalSettings = chain -> withInternational(international, chain);
        if (international != null) {
            hookMethod(module, loader, CREDENTIALS + ".DefaultCombinedPicker", "setDefaultKey",
                    regionalSettings, String.class);
            hookMethod(module, loader, CREDENTIALS + ".DefaultCombinedPreferenceController",
                    "getCombinedProviderInfos", regionalSettings, CredentialManager.class, int.class);
        }
        boolean bindSwitch = Build.VERSION.SDK_INT >= 36;
        if (international == null && !bindSwitch) return;
        try (var bridge = openDexKit(SETTINGS, loader, app)) {
            if (international != null) {
                String open = "Lcom/android/settings/applications/credentials/CombinedProviderInfo;"
                        + "->launchSettingsActivityIntent(Landroid/content/Context;Ljava/lang/CharSequence;"
                        + "Ljava/lang/CharSequence;I)";
                try {
                    var rows = bridge.getMethods(FindMethod.create().searchPackages(CREDENTIALS)
                            .matcher(MethodMatcher.create().name("onLeftSideClicked").paramCount(0)
                                    .anyOf(MethodMatcher.create().addInvoke(open + "Z"),
                                            MethodMatcher.create().addInvoke(open + "V"))));
                    for (DexMethod row : rows) {
                        try {
                            hook(module, row.getMethodInstance(loader), regionalSettings);
                        } catch (ReflectiveOperationException error) {
                            Log.w(TAG, "Credential row unavailable: " + row, error);
                        }
                    }
                } catch (Exception error) {
                    Log.w(TAG, "Credential row query failed", error);
                }
            }
            if (bindSwitch) installCredentialSwitch(module, loader, bridge);
        } catch (Throwable error) {
            Log.w(TAG, "Settings DexKit setup failed", error);
        }
    }
    private static Field internationalFlag(ClassLoader loader) {
        try {
            Field field = R.findField(loader.loadClass("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
            if (field.getType() == boolean.class && Modifier.isStatic(field.getModifiers())) return field;
            Log.w(TAG, "Unexpected MIUI region flag: " + field);
        } catch (ReflectiveOperationException error) {
            Log.w(TAG, "MIUI region flag unavailable", error);
        }
        return null;
    }
    private static void installCredentialSwitch(XposedModule module, ClassLoader loader,
            DexKitCacheBridge.RecyclableBridge bridge) throws Exception {
        Class<?> preference = loader.loadClass(CREDENTIALS
                + ".CredentialManagerPreferenceController$CombiPreference");
        Method bind = preference.getDeclaredMethod("onBindViewHolder",
                loader.loadClass("androidx.preference.PreferenceViewHolder"));
        if (bind.getReturnType() != void.class) return;
        DexMethod target = bridge.getMethod(FindMethod.create().searchPackages(CREDENTIALS)
                .matcher(MethodMatcher.create().name("onCheckChanged").returnType(boolean.class)
                        .paramTypes(preference, boolean.class)
                        .anyOf(MethodMatcher.create().addInvoke(CONTROLLER + "togglePackageNameEnabled(Ljava/lang/String;)Z"),
                                MethodMatcher.create().addInvoke(CONTROLLER + "togglePackageNameDisabled(Ljava/lang/String;)V"))));
        Method callback = target.getMethodInstance(loader);
        callback.setAccessible(true);
        Field button = R.findField(preference, "mSwitch");
        Field checked = R.findField(preference, "mChecked");
        Field listener = R.findField(preference, "mOnClickListener");
        Field itemView = R.findField(bind.getParameterTypes()[0], "itemView");
        int switchId = R.findField(loader.loadClass("com.android.settingslib.R$id"), "switchWidget").getInt(null);
        Method description = preference.getDeclaredMethod("maybeUpdateContentDescription");
        description.setAccessible(true);
        hook(module, bind, chain -> {
            Object result = chain.proceed();
            Object row = chain.getThisObject();
            try {
                if (button.get(row) == null && itemView.get(chain.getArg(0)) instanceof View view
                        && view.findViewById(switchId) instanceof CompoundButton toggle) {
                    toggle.setChecked(checked.getBoolean(row));
                    writeField(button, row, toggle);
                    toggle.setOnClickListener(ignored -> {
                        try {
                            boolean original = checked.getBoolean(row);
                            Object clickListener = listener.get(row);
                            if (!enabled() || !callback.getDeclaringClass().isInstance(clickListener)) {
                                toggle.setChecked(original);
                                return;
                            }
                            if (!Boolean.TRUE.equals(callback.invoke(clickListener, row, toggle.isChecked()))) {
                                writeField(checked, row, original);
                                toggle.setChecked(original);
                            }
                            description.invoke(row);
                        } catch (Exception error) {
                            try {
                                toggle.setChecked(checked.getBoolean(row));
                                description.invoke(row);
                            } catch (Exception restoreError) {
                                if (restoreError != error) error.addSuppressed(restoreError);
                            }
                            Log.w(TAG, "Credential switch callback failed", error);
                        }
                    });
                    description.invoke(row);
                }
            } catch (Exception error) {
                Log.w(TAG, "Credential switch binding failed", error);
            }
            return result;
        });
    }
    private static void installSecurityCenter(XposedModule module, ClassLoader loader,
            ApplicationInfo app) throws Exception {
        try (DexKitCacheBridge.RecyclableBridge bridge = openDexKit(SECURITY_CENTER, loader, app)) {
            List<ClassData> classes = new ArrayList<>();
            bridge.withBridge(raw -> {
                ClassData application = raw.getClassData("Lcom/miui/securitycenter/Application;");
                if (application != null) classes.add(application);
                ClassData cacheService = raw.getClassData("Lcom/miui/securitycenter/service/CacheService;");
                if (cacheService != null) classes.add(cacheService);
            });
            if (classes.isEmpty()) throw new ClassNotFoundException("Security Center DEX classes unavailable");
            for (boolean credentials : new boolean[]{false, true}) {
                String label = credentials ? "credential" : "autofill";
                try {
                    DexMethod helper = findProviderHelper(bridge, classes, credentials);
                    DexMethod writer = findProviderWriter(bridge, classes, helper, credentials);
                    Method method = writer.getMethodInstance(loader);
                    if (method.getReturnType() != void.class) {
                        throw new NoSuchMethodException("Expected void provider writer: " + method);
                    }
                    hook(module, method, chain -> null);
                } catch (Throwable error) {
                    Log.w(TAG, "Security Center " + label + " reset target unavailable", error);
                }
            }
        }
    }
    private static DexMethod findProviderHelper(DexKitCacheBridge.RecyclableBridge bridge,
            List<ClassData> classes, boolean credentials) {
        String resourceCall = credentials ? "getStringArray(I)[Ljava/lang/String;"
                : "getString(I)Ljava/lang/String;";
        String secureWrite = "Landroid/provider/Settings$Secure;->putString"
                + "(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;)Z";
        String secureWriteForUser = "Landroid/provider/Settings$Secure;->putStringForUser"
                + "(Landroid/content/ContentResolver;Ljava/lang/String;Ljava/lang/String;I)Z";
        return bridge.getMethod(FindMethod.create().searchInClass(classes)
                .matcher(MethodMatcher.create()
                        .anyOf(MethodMatcher.create().paramTypes(Context.class, String.class, int.class),
                                MethodMatcher.create().paramTypes(String.class, int.class))
                        .addInvoke("Landroid/content/res/Resources;->" + resourceCall)
                        .allOf(MethodMatcher.create().anyOf(MethodMatcher.create().addInvoke(secureWrite),
                                MethodMatcher.create().addInvoke(secureWriteForUser)))));
    }
    private static DexMethod findProviderWriter(DexKitCacheBridge.RecyclableBridge bridge,
            List<ClassData> classes, DexMethod helper, boolean credentials) {
        MethodMatcher matcher = MethodMatcher.create().returnType(void.class).addInvoke(helper.toString());
        if (credentials) matcher.usingEqStrings(
                "credential_service", "credential_service_primary", "is_credential_configured");
        else matcher.addEqString("autofill_service");
        return bridge.getMethod(FindMethod.create().searchInClass(classes)
                .matcher(matcher));
    }
    private static void installScanner(XposedModule module, ClassLoader loader) throws Exception {
        Method method = loader.loadClass("com.xiaomi.scanner.module.code.utils.bean.MiFiDoBean")
                .getDeclaredMethod("getAppPackageName");
        if (method.getReturnType() == String.class) hook(module, method, chain -> "");
    }
    private static DexKitCacheBridge.RecyclableBridge openDexKit(String packageName,
            ClassLoader loader, ApplicationInfo app) throws Exception {
        Context context = applicationContext();
        if (context == null) throw new IllegalStateException("Host context unavailable for DexKit");
        if (!packageName.equals(context.getPackageName())) context = context.createPackageContext(packageName, 0);
        Entry.loadDexKitLibrary();
        if (!dexKitInitialized) {
            DexKitCacheBridge.init(MemoryCache.INSTANCE);
            dexKitInitialized = true;
        }
        String version;
        try {
            var info = context.getPackageManager().getPackageInfo(packageName, 0);
            version = info.versionName + "-" + info.getLongVersionCode();
        } catch (PackageManager.NameNotFoundException error) {
            version = app == null ? "unknown" : app.sourceDir;
        }
        return DexKitCacheBridge.create(packageName + ":" + version, loader);
    }
    private static void installCredentialSessions(XposedModule module, ClassLoader loader) {
        for (String name : new String[]{"com.android.server.credentials.ProviderGetSession",
                "com.android.server.credentials.ProviderCreateSession"}) {
            try {
                for (Method method : loader.loadClass(name).getDeclaredMethods()) {
                    if (method.getName().equals("createNewSession")) deoptimize(module, method);
                }
            } catch (Throwable error) {
                Log.w(TAG, "Credential caller deoptimization skipped: " + name, error);
            }
        }
        try {
            Class<?> requestSession = loader.loadClass("com.android.server.credentials.RequestSession");
            Field hybridService = R.findField(requestSession, "mHybridService");
            if (hybridService.getType() != String.class || Modifier.isStatic(hybridService.getModifiers())) return;
            Class<?> lifetime = loader.loadClass(
                    "com.android.server.credentials.RequestSession$SessionLifetime");
            for (Constructor<?> constructor : requestSession.getDeclaredConstructors()) {
                Class<?>[] parameters = constructor.getParameterTypes();
                if (parameters.length >= 10 && parameters[0] == Context.class
                        && parameters[1] == lifetime) {
                    hook(module, constructor, chain -> {
                        Object result = chain.proceed();
                        try {
                            writeField(hybridService, chain.getThisObject(),
                                    "com.google.android.gms/.auth.api.credentials.credman.service.RemoteService");
                        } catch (Exception error) {
                            Log.w(TAG, "Hybrid credential provider unchanged", error);
                        }
                        return result;
                    });
                }
            }
        } catch (Throwable error) {
            Log.w(TAG, "RequestSession variant unavailable", error);
        }
    }
    private static void installChooser(XposedModule module, ClassLoader loader) {
        try {
            String result = "android.credentials.selection.IntentCreationResult";
            Class<?> factory = loader.loadClass("android.credentials.selection.IntentFactory");
            Class<?> builder = loader.loadClass(result + "$Builder");
            Field success = loader.loadClass(result + "$OemUiUsageStatus").getField("SUCCESS");
            Method setPackage = builder.getMethod("setOemUiPackageName", String.class);
            Method setStatus = builder.getMethod("setOemUiUsageStatus", success.getType());
            Object successValue = success.get(null);
            XposedInterface.Hooker chooser = chain -> {
                if (chain.getArg(0) instanceof Context context
                        && builder.isInstance(chain.getArg(1))) {
                    try {
                        if (googleChooserAvailable(context.getPackageManager())) {
                            Object target = chain.getArg(1);
                            setStatus.invoke(target, successValue);
                            setPackage.invoke(target, GOOGLE_CHOOSER.getPackageName());
                            return GOOGLE_CHOOSER;
                        }
                    } catch (Exception error) {
                        Log.w(TAG, "Google credential chooser unavailable", error);
                    }
                }
                return chain.proceed();
            };
            for (Method method : factory.getDeclaredMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals("getOemOverrideComponentName")
                        && method.getReturnType() == ComponentName.class
                        && (params.length == 2 || params.length == 3 && params[2] == int.class)
                        && params[0] == Context.class && params[1] == builder) hook(module, method, chooser);
            }
        } catch (ReflectiveOperationException | LinkageError error) {
            Log.w(TAG, "Credential chooser variant unavailable", error);
        }
    }
    private static boolean googleChooserAvailable(PackageManager manager)
            throws PackageManager.NameNotFoundException {
        ActivityInfo activity = manager.getActivityInfo(GOOGLE_CHOOSER,
                PackageManager.ComponentInfoFlags.of(PackageManager.MATCH_SYSTEM_ONLY));
        int state = manager.getComponentEnabledSetting(GOOGLE_CHOOSER);
        return activity.exported && (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && activity.enabled);
    }
    private static void hookMethod(XposedModule module, ClassLoader loader, String owner,
            String name, XposedInterface.Hooker hooker, Class<?>... parameters) {
        try {
            hook(module, loader.loadClass(owner).getDeclaredMethod(name, parameters), hooker);
        } catch (ReflectiveOperationException | LinkageError error) {
            Log.w(TAG, "Hook target unavailable: " + owner + "#" + name, error);
        }
    }
    private static void hook(XposedModule module, Executable target,
            XposedInterface.Hooker hooker) {
        if (installed.contains(target) || Modifier.isAbstract(target.getModifiers())) return;
        try {
            target.setAccessible(true);
            deoptimize(module, target);
            Reflect.attach(target, chain -> enabled() ? hooker.intercept(chain) : chain.proceed(),
                    XposedInterface.ExceptionMode.PROTECTIVE);
            installed.add(target);
            Log.i(TAG, target.toGenericString());
        } catch (Throwable error) {
            Log.w(TAG, "Hook setup failed: " + target, error);
        }
    }
    private static void deoptimize(XposedModule module, Executable target) {
        try {
            module.deoptimize(target);
        } catch (Throwable error) {
            Log.w(TAG, "Deoptimization skipped: " + target, error);
        }
    }
    private static synchronized Object withInternational(Field field, XposedInterface.Chain chain)
            throws Throwable {
        boolean original;
        try {
            original = field.getBoolean(null);
            if (!original) writeField(field, null, true);
        } catch (Exception error) {
            Log.w(TAG, "MIUI region flag unchanged", error);
            return chain.proceed();
        }
        try {
            return chain.proceed();
        } finally {
            if (!original) {
                try {
                    writeField(field, null, false);
                } catch (Exception error) {
                    Log.w(TAG, "Cannot restore MIUI region flag", error);
                }
            }
        }
    }
    private static boolean enabled() {
        try {
            Context context = applicationContext();
            return context != null && Settings.Global.getInt(context.getContentResolver(), SETTING, 0) == 1;
        } catch (Exception | LinkageError ignored) {
            return false;
        }
    }
    private static Context applicationContext() {
        if (Config.context != null) return Config.context;
        try {
            Context application = ActivityThread.currentApplication();
            if (application != null) return application;
        } catch (RuntimeException | LinkageError ignored) { }
        try {
            ActivityThread thread = ActivityThread.currentActivityThread();
            return thread == null ? null : thread.getSystemContext();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }
    private static void writeField(Field field, Object target, Object value) throws IllegalAccessException {
        try {
            field.set(target, value);
        } catch (IllegalAccessException denied) {
            FieldAccess access = FieldAccess.ART;
            if (access == null) throw denied;
            // Resolve the field before reading its ART metadata. Never probe object memory blindly.
            field.get(target);
            Unsafe unsafe = access.unsafe();
            long offset = unsafe.getInt(field, access.offset());
            Object base = Modifier.isStatic(field.getModifiers()) ? field.getDeclaringClass() : target;
            if (field.getType() == boolean.class) unsafe.putBoolean(base, offset, (Boolean) value);
            else if (!field.getType().isPrimitive() && field.getType().isInstance(value)) {
                unsafe.putObjectVolatile(base, offset, value);
            } else throw denied;
        }
    }
    private record FieldAccess(Unsafe unsafe, long offset) {
        private static final FieldAccess ART = find();
        private static FieldAccess find() {
            try {
                Unsafe unsafe = (Unsafe) R.findField(Unsafe.class, "theUnsafe").get(null);
                Field offset = R.findField(Field.class, "offset");
                if (offset.getType() == int.class && !Modifier.isStatic(offset.getModifiers())) {
                    return new FieldAccess(unsafe, unsafe.objectFieldOffset(offset));
                }
            } catch (Exception | LinkageError ignored) { }
            return null;
        }
    }
    private static final class MemoryCache implements DexKitCacheBridge.Cache {
        private static final MemoryCache INSTANCE = new MemoryCache();
        private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, List<String>> lists = new ConcurrentHashMap<>();
        private MemoryCache() { }
        @Override public String getString(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }
        @Override public void putString(String key, String value) { values.put(key, value); }
        @Override public List<String> getStringList(String key, List<String> defaultValue) {
            return lists.getOrDefault(key, defaultValue);
        }
        @Override public void putStringList(String key, List<String> value) { lists.put(key, value); }
        @Override public void remove(String key) {
            values.remove(key);
            lists.remove(key);
        }
        @Override public Collection<String> getAllKeys() {
            Set<String> keys = new HashSet<>(values.keySet());
            keys.addAll(lists.keySet());
            return keys;
        }
        @Override public void clearAll() {
            values.clear();
            lists.clear();
        }
    }
}
