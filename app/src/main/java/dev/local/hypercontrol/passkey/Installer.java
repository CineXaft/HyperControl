package dev.local.hypercontrol.passkey;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class Installer {
    private static boolean appInstalled;
    private static boolean serverInstalled;

    private static void attach(XposedModule xposedModule, Executable executable, int i, Field field) {
        executable.setAccessible(true);
        try {
            xposedModule.deoptimize(executable);
        } catch (Throwable th) {
            Policy.warn("Passkey target deoptimization skipped", th);
        }
        Interceptor interceptor = new Interceptor(i, field);
        xposedModule.hook(executable).intercept(interceptor);
        Log.i("HyperControl.Passkey", executable.toGenericString());
    }

    private static void deoptCallers(XposedModule xposedModule, ClassLoader classLoader) {
        for (String str : new String[]{"com.android.server.credentials.ProviderGetSession", "com.android.server.credentials.ProviderCreateSession"}) {
            try {
                for (Method method : classLoader.loadClass(str).getDeclaredMethods()) {
                    if ("createNewSession".equals(method.getName())) {
                        xposedModule.deoptimize(method);
                    }
                }
            } catch (Throwable th) {
                Policy.warn("Credential caller deoptimization skipped", th);
            }
        }
    }

    private static void named(XposedModule xposedModule, Class cls, String str, int i, Field field) {
        for (Method method : cls.getDeclaredMethods()) {
            if (str.equals(method.getName())) {
                try {
                    attach(xposedModule, method, i, field);
                } catch (Throwable th) {
                    Policy.warn(str, th);
                }
            }
        }
    }

    public static synchronized void packages(XposedModule xposedModule, String str, ClassLoader classLoader) {
        if (appInstalled) {
            return;
        }
        if ("com.android.settings".equals(str)) {
            appInstalled = true;
            Field fieldFind = null;
            try {
                fieldFind = Fields.find(classLoader.loadClass("miui.os.Build"), "IS_INTERNATIONAL_BUILD");
            } catch (Throwable th) {
                Policy.warn("MIUI build flag missing", th);
            }
            for (String str2 : "com.android.settings.applications.credentials.DefaultCombinedPicker#setDefaultKey|com.android.settings.applications.credentials.DefaultCombinedPreferenceController#getCombinedProviderInfos|com.android.settings.applications.credentials.CombinedProviderInfo#launchSettingsActivityIntent".split("\\|")) {
                try {
                    String[] strArrSplit = str2.split("#");
                    named(xposedModule, classLoader.loadClass(strArrSplit[0]), strArrSplit[1], 0, fieldFind);
                } catch (Throwable th) {
                    Policy.warn("Credential settings variant not present", th);
                }
            }
            try {
                named(xposedModule, classLoader.loadClass("com.android.settings.applications.credentials.CredentialManagerPreferenceController$CombiPreference"), "onBindViewHolder", 5, null);
            } catch (Throwable th) {
                Policy.warn("Passkey package compatibility target unavailable", th);
            }
            return;
        }
        if ("com.miui.securitycenter".equals(str)) {
            appInstalled = true;
            try {
                for (Method method : Settings.Secure.class.getDeclaredMethods()) {
                    String name = method.getName();
                    if (("putString".equals(name) || "putStringForUser".equals(name)) && method.getReturnType() == Boolean.TYPE) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length >= 3 && parameterTypes[0] == ContentResolver.class && parameterTypes[1] == String.class && parameterTypes[2] == String.class) {
                            attach(xposedModule, method, 4, null);
                        }
                    }
                }
            } catch (Throwable th) {
                Policy.warn("Passkey package compatibility target unavailable", th);
            }
            return;
        }
        if ("com.xiaomi.scanner".equals(str)) {
            appInstalled = true;
            try {
                named(xposedModule, classLoader.loadClass("com.xiaomi.scanner.module.code.utils.bean.MiFiDoBean"), "getAppPackageName", 3, null);
            } catch (Throwable th) {
                Policy.warn("Passkey package compatibility target unavailable", th);
            }
        }
    }

    public static synchronized void server(XposedModule xposedModule, ClassLoader classLoader) {
        Class<?>[] parameterTypes;
        int length;
        if (serverInstalled) {
            return;
        }
        serverInstalled = true;
        deoptCallers(xposedModule, classLoader);
        try {
            Class<?> clsLoadClass = classLoader.loadClass("com.android.server.credentials.RequestSession");
            Field fieldFind = Fields.find(clsLoadClass, "mHybridService");
            for (Constructor<?> constructor : clsLoadClass.getDeclaredConstructors()) {
                Class<?>[] parameterTypes2 = constructor.getParameterTypes();
                if (parameterTypes2.length >= 10 && parameterTypes2[0] == Context.class && "com.android.server.credentials.RequestSession$SessionLifetime".equals(parameterTypes2[1].getName())) {
                    attach(xposedModule, constructor, 1, fieldFind);
                }
            }
        } catch (Throwable th) {
            Policy.warn("RequestSession variant unavailable", th);
        }
        try {
            for (Method method : classLoader.loadClass("android.credentials.selection.IntentFactory").getDeclaredMethods()) {
                if ("getOemOverrideComponentName".equals(method.getName()) && method.getReturnType() == ComponentName.class && (length = (parameterTypes = method.getParameterTypes()).length) >= 2 && length <= 3 && parameterTypes[0] == Context.class && "android.credentials.selection.IntentCreationResult$Builder".equals(parameterTypes[1].getName()) && (length != 3 || parameterTypes[2] == Integer.TYPE)) {
                    attach(xposedModule, method, 2, null);
                }
            }
        } catch (Throwable th2) {
            Policy.warn("IntentFactory variant unavailable", th2);
        }
    }
}
