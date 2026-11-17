package com.cinexaft.hypercontrol.securitycenter;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import com.cinexaft.hypercontrol.Reflect;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
public final class SecurityCenter {
    private static volatile long adbThreadId;
    private static boolean equalsHooked;
    private static boolean[] flags;
    private static ClassLoader loader;
    private static boolean scoreHooked;

    public static boolean adbActive() {
        return Thread.currentThread().getId() != adbThreadId;
    }

    public static void bind(Context context) {
        boolean[] zArr = new boolean[3];
        flags = zArr;
        ContentResolver contentResolver = context.getContentResolver();
        if (Settings.Global.getInt(contentResolver, "hypercontrol_securitycenter_score_zero", 0) != 0) {
            zArr[0] = true;
        }
        if (Settings.Global.getInt(contentResolver, "hypercontrol_securitycenter_intercept_bypass", 0) != 0) {
            zArr[1] = true;
        }
        if (Settings.Global.getInt(contentResolver, "hypercontrol_securitycenter_adb_verify_bypass", 0) != 0) {
            zArr[2] = true;
        }
        if (enabled(2)) {
            installEqualsHook();
        }
    }

    public static void discoverScore(String str) {
        if (scoreHooked) {
            return;
        }
        try {
            hookScore(loader.loadClass("com.miui.securityscan.scanner.ScoreManager").getDeclaredMethod(str, new Class[0]));
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    public static boolean enabled(int i) {
        boolean[] zArr = flags;
        if (zArr != null) {
            return zArr[i];
        }
        return false;
    }

    private static Method findScoreMethod(ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> clsLoadClass = classLoader.loadClass("com.miui.securityscan.scanner.ScoreManager");
        Class<?>[] clsArr = new Class[0];
        try {
            return clsLoadClass.getDeclaredMethod("q", clsArr);
        } catch (Throwable th) {
            try {
                return clsLoadClass.getDeclaredMethod("o", clsArr);
            } catch (Throwable th2) {
                return null;
            }
        }
    }

    public static void hookScore(Method method) {
        if (!scoreHooked && method.getReturnType() == Integer.TYPE && method.getParameterTypes().length == 0) {
            Reflect.hook(method, new SecurityHook(0));
            scoreHooked = true;
        }
    }

    public static void install(ClassLoader classLoader) {
        loader = classLoader;
        try {
            Reflect.hook(Application.class.getDeclaredMethod("attach", Context.class), new SecurityHook(4));
        } catch (Throwable th) {
            Reflect.log(th);
        }
        try {
            installScore(classLoader);
        } catch (Throwable th2) {
            Reflect.log(th2);
        }
        try {
            installScoreKeyword(classLoader);
        } catch (Throwable th3) {
            Reflect.log(th3);
        }
        try {
            installFragments(classLoader);
        } catch (Throwable th4) {
            Reflect.log(th4);
        }
        try {
            installAdb(classLoader);
        } catch (Throwable th5) {
            Reflect.log(th5);
        }
    }

    private static void installAdb(ClassLoader classLoader) throws ClassNotFoundException, NoSuchMethodException {
        Reflect.hook(classLoader.loadClass("com.miui.permcenter.install.AdbInstallVerifyActivity").getDeclaredMethod("onCreate", Bundle.class), new SecurityHook(2));
    }

    private static void installEqualsHook() {
        if (equalsHooked) {
            return;
        }
        try {
            Reflect.hook(String.class.getDeclaredMethod("equals", Object.class), new SecurityHook(5));
            equalsHooked = true;
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    private static void installFragments(ClassLoader classLoader) throws ClassNotFoundException {
        for (String str : new String[]{"com.miui.permcenter.privacymanager.InterceptBaseFragment", "com.miui.permcenter.privacymanager.InterceptMIUIFragment", "com.miui.permcenter.privacymanager.InterceptPermissionFragment"}) {
            for (Method method : classLoader.loadClass(str).getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isProtected(modifiers) && !Modifier.isAbstract(modifiers) && method.getReturnType() == Integer.TYPE && method.getParameterTypes().length == 0) {
                    Reflect.hook(method, new SecurityHook(1));
                }
            }
        }
    }

    private static void installScore(ClassLoader classLoader) throws ClassNotFoundException {
        Method methodFindScoreMethod = findScoreMethod(classLoader);
        if (methodFindScoreMethod == null || methodFindScoreMethod.getReturnType() != Integer.TYPE) {
            return;
        }
        hookScore(methodFindScoreMethod);
    }

    private static void installScoreKeyword(ClassLoader classLoader) {
        try {
            Reflect.hook(Log.class.getDeclaredMethod("d", String.class, String.class), new SecurityHook(6));
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    public static boolean isBypassString(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 5) {
            return str.charAt(0) == 'h' && str.charAt(1) == 'o' && str.charAt(2) == 'u' && str.charAt(3) == 'j' && str.charAt(4) == 'i';
        }
        return length == 3 && str.charAt(0) == 'e' && str.charAt(1) == 'e' && str.charAt(2) == 'a';
    }

    public static void setAdbActive(boolean z) {
        if (z) {
            adbThreadId = Thread.currentThread().getId();
        } else {
            adbThreadId = 0L;
        }
    }
}
