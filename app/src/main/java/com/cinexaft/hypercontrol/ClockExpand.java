package com.cinexaft.hypercontrol;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class ClockExpand {
    private static final String TAG = "HyperControl.Clock";

    /** Caller sites that must be deoptimized so the clock animator takes the hooks below. */
    private static final String[] DEOPT = {
        "com.android.keyguard.panel.KeyguardPanelViewController#access$updateNotificationState",
        "com.android.keyguard.clock.KeyguardClockContainer#doAnimationToAod",
        "com.android.keyguard.clock.animation.AnimationHelper$1#update",
        "com.android.keyguard.interactor.KeyguardClockNotifInteractor$collectNotificationYSource$1$invokeSuspend$$inlined$map$1$2#emit",
        "com.android.keyguard.interactor.KeyguardClockNotifInteractor#setNotifY",
        "com.android.keyguard.interactor.KeyguardClockNotifInteractor#orientationChange",
        "com.android.keyguard.panel.KeyguardPanelViewController$onLockScreenInfoChange$1#run",
        "com.android.keyguard.panel.KeyguardPanelViewController$2$1#emit",
        "com.android.keyguard.panel.KeyguardPanelViewController$statusBarStateControllerCallback$1#onStateChanged",
    };

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

    private static void deoptCaller(ClassLoader classLoader, String spec) {
        try {
            int split = spec.indexOf('#');
            Class<?> owner = classLoader.loadClass(spec.substring(0, split));
            String name = spec.substring(split + 1);
            for (Method method : owner.getDeclaredMethods()) {
                if (name.equals(method.getName())) {
                    Entry.module.deoptimize(method);
                }
            }
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field declaredField = owner.getDeclaredField(name);
        declaredField.setAccessible(true);
        return declaredField;
    }

    public static synchronized void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        if (Platform.clockUnsupported()) {
            installed = true;
            Log.i(TAG, "clock feature disabled on HyperOS 2/3");
            return;
        }
        Log.i(TAG, "install called");
        installed = true;
        try {
            Class<?> animationHelper = classLoader.loadClass("com.android.keyguard.clock.animation.AnimationHelper");
            Class<?> panel = classLoader.loadClass("com.android.keyguard.panel.KeyguardPanelViewController");
            Class<?> injector = classLoader.loadClass("com.android.keyguard.injector.KeyguardClockInjector");
            Class<?> container = classLoader.loadClass("com.android.keyguard.clock.KeyguardClockContainer");
            Class<?> notifInteractor = classLoader.loadClass("com.android.keyguard.interactor.KeyguardClockNotifInteractor");
            Class<?> consistency = classLoader.loadClass("com.android.keyguard.dfx.clockstyle.KeyguardStyleConsistencyMonitor");
            fHasNotif = field(animationHelper, "mHasNotification");
            fInjector = field(panel, "keyguardClockInjector");
            fClockView = field(injector, "keyguardClockView");
            fContainerNotif = field(container, "mHasNotification");
            fMonitor = field(panel, "keyguardStyleConsistencyMonitor");
            fMonitorNotif = field(consistency, "hasNotification");
            fRawY = field(notifInteractor, "rawNotifY");
            fEditRect = field(notifInteractor, "maxEditRect");
            fMargin = field(notifInteractor, "clockNotificationMargin");
            fContext = field(notifInteractor, "context");
            Method height = classLoader.loadClass("com.miui.utils.configs.MiuiConfigs").getDeclaredMethod("getScreenHeight", Context.class);
            height.setAccessible(true);
            mHeight = height;
            hook(animationHelper, "doAnimationToNotification", 2, 0);
            hook(animationHelper, "doAnimationToAod", 5, 1);
            hook(panel, "access$updateNotificationState", 2, 2);
            hook(notifInteractor, "access$correctYValue", 2, 3);
            hook(notifInteractor, "updateNotifYParam", 1, 4);
            for (String spec : DEOPT) {
                deoptCaller(classLoader, spec);
            }
        } catch (Throwable error) {
            installed = false;
            Reflect.log(error);
        }
    }

    private static void hook(Class<?> owner, String name, int count, int kind) throws NoSuchMethodException {
        Method method = Hooks.byNameCount(owner, name, count);
        if (method == null) {
            throw new NoSuchMethodException(name);
        }
        Hooks.install(method, new ClockHook(kind), TAG);
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
