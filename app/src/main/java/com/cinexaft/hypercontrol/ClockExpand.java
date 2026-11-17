package com.cinexaft.hypercontrol;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/* JADX INFO: loaded from: classes.dex */
public final class ClockExpand {
    public static Field fClockView;
    public static Field fContainerNotif;
    public static Field fContext;
    public static Field fEditRect;
    public static Field fHasNotif;
    public static Field fInjector;
    public static Field fMargin;
    public static Field fMonitor;
    public static Field fMonitorNotif;
    public static Field fRawY;
    private static boolean installed;
    private static Method mHeight;

    private static void d(ClassLoader classLoader, String str, String str2) {
        try {
            for (Method method : classLoader.loadClass(str).getDeclaredMethods()) {
                if (str2.equals(method.getName())) {
                    Entry.module.deoptimize(method);
                }
            }
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    private static Field f(Class cls, String str) throws NoSuchFieldException {
        Field declaredField = cls.getDeclaredField(str);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private static void h(Method method, int i) {
        Entry.module.deoptimize(method);
        Reflect.hook(method, new ClockHook(i));
        Log.i("HyperControl.Clock", method.toString());
    }

    public static synchronized void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        if (Platform.clockUnsupported()) {
            installed = true;
            Log.i("HyperControl.Clock", "clock feature disabled on HyperOS 2/3");
            return;
        }
        Log.i("HyperControl.Clock", "install called");
        installed = true;
        try {
            Class<?> clsLoadClass = classLoader.loadClass("com.android.keyguard.clock.animation.AnimationHelper");
            Class<?> clsLoadClass2 = classLoader.loadClass("com.android.keyguard.panel.KeyguardPanelViewController");
            Class<?> clsLoadClass3 = classLoader.loadClass("com.android.keyguard.injector.KeyguardClockInjector");
            Class<?> clsLoadClass4 = classLoader.loadClass("com.android.keyguard.clock.KeyguardClockContainer");
            Class<?> clsLoadClass5 = classLoader.loadClass("com.android.keyguard.interactor.KeyguardClockNotifInteractor");
            Class<?> clsLoadClass6 = classLoader.loadClass("com.android.keyguard.dfx.clockstyle.KeyguardStyleConsistencyMonitor");
            fHasNotif = f(clsLoadClass, "mHasNotification");
            fInjector = f(clsLoadClass2, "keyguardClockInjector");
            fClockView = f(clsLoadClass3, "keyguardClockView");
            fContainerNotif = f(clsLoadClass4, "mHasNotification");
            fMonitor = f(clsLoadClass2, "keyguardStyleConsistencyMonitor");
            fMonitorNotif = f(clsLoadClass6, "hasNotification");
            fRawY = f(clsLoadClass5, "rawNotifY");
            fEditRect = f(clsLoadClass5, "maxEditRect");
            fMargin = f(clsLoadClass5, "clockNotificationMargin");
            fContext = f(clsLoadClass5, "context");
            Method declaredMethod = classLoader.loadClass("com.miui.utils.configs.MiuiConfigs").getDeclaredMethod("getScreenHeight", Context.class);
            declaredMethod.setAccessible(true);
            mHeight = declaredMethod;
            h(m(clsLoadClass, "doAnimationToNotification", 2), 0);
            h(m(clsLoadClass, "doAnimationToAod", 5), 1);
            h(m(clsLoadClass2, "access$updateNotificationState", 2), 2);
            h(m(clsLoadClass5, "access$correctYValue", 2), 3);
            h(m(clsLoadClass5, "updateNotifYParam", 1), 4);
            d(classLoader, "com.android.keyguard.panel.KeyguardPanelViewController", "access$updateNotificationState");
            d(classLoader, "com.android.keyguard.clock.KeyguardClockContainer", "doAnimationToAod");
            d(classLoader, "com.android.keyguard.clock.animation.AnimationHelper$1", "update");
            d(classLoader, "com.android.keyguard.interactor.KeyguardClockNotifInteractor$collectNotificationYSource$1$invokeSuspend$$inlined$map$1$2", "emit");
            d(classLoader, "com.android.keyguard.interactor.KeyguardClockNotifInteractor", "setNotifY");
            d(classLoader, "com.android.keyguard.interactor.KeyguardClockNotifInteractor", "orientationChange");
            d(classLoader, "com.android.keyguard.panel.KeyguardPanelViewController$onLockScreenInfoChange$1", "run");
            d(classLoader, "com.android.keyguard.panel.KeyguardPanelViewController$2$1", "emit");
            d(classLoader, "com.android.keyguard.panel.KeyguardPanelViewController$statusBarStateControllerCallback$1", "onStateChanged");
        } catch (Throwable th) {
            installed = false;
            Reflect.log(th);
        }
    }

    private static Method m(Class cls, String str, int i) throws NoSuchMethodException {
        for (Method method : cls.getDeclaredMethods()) {
            if (str.equals(method.getName()) && method.getParameterTypes().length == i) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(str);
    }

    public static Float noNotifY(Object obj) throws IllegalAccessException, InvocationTargetException {
        int iIntValue;
        fRawY.setFloat(obj, Float.MAX_VALUE);
        int i = ((Rect) fEditRect.get(obj)).bottom;
        if (i > 0) {
            iIntValue = i + fMargin.getInt(obj);
        } else {
            iIntValue = ((Integer) mHeight.invoke(null, fContext.get(obj))).intValue();
        }
        return Float.valueOf(iIntValue);
    }
}
