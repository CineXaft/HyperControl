package com.cinexaft.hypercontrol;

import android.view.View;
import android.view.ViewGroup;
import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class SignalHook implements XposedInterface.Hooker, View.OnLayoutChangeListener {
    private static int[] ids;
    private static Class mobileViewClass;
    private static Class wifiViewClass;

    private static View child(View view, String str, int i) {
        int[] iArr = ids;
        if (iArr == null) {
            iArr = new int[3];
            ids = iArr;
        }
        int iId = Platform.id(view, str, iArr, i);
        if (iId > 0) {
            return view.findViewById(iId);
        }
        return null;
    }

    private static void collapse(View view) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams == null || (layoutParams.width | layoutParams.height) == 0) {
            return;
        }
        layoutParams.width = 0;
        layoutParams.height = 0;
        view.setLayoutParams(layoutParams);
    }

    public static void install(ClassLoader classLoader) {
        try {
            mobileViewClass = loadClass(classLoader, "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView");
            wifiViewClass = loadClass(classLoader, "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView");
            Reflect.hook(classLoader.loadClass("com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView").getDeclaredMethod("initView", String.class, classLoader.loadClass("kotlin.jvm.functions.Function0")), new SignalHook());
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    private static Class loadClass(ClassLoader classLoader, String str) {
        try {
            return classLoader.loadClass(str);
        } catch (Throwable th) {
            return null;
        }
    }

    private static void transparent(View view, String str, int i, int i2, int i3) {
        View viewChild;
        if ((i & i2) == 0 || (viewChild = child(view, str, i3)) == null) {
            return;
        }
        viewChild.setAlpha(0.0f);
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        View viewChild;
        Object objProceed = chain.proceed();
        int iSignalMask = State.signalMask();
        if (iSignalMask != 0) {
            try {
                View view = (View) chain.getThisObject();
                Class<?> cls = view.getClass();
                if (cls != mobileViewClass) {
                    if (cls == wifiViewClass) {
                        transparent(view, "wifi_activity", iSignalMask, 2, 1);
                        transparent(view, "wifi_standard", iSignalMask, 4, 2);
                    }
                } else if ((iSignalMask & 1) != 0 && (viewChild = child(view, "mobile_left_mobile_inout", 0)) != null) {
                    collapse(viewChild);
                    viewChild.addOnLayoutChangeListener(this);
                }
            } catch (Throwable th) {
                Reflect.log(th);
                return objProceed;
            }
        }
        return objProceed;
    }

    @Override // android.view.View.OnLayoutChangeListener
    public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        collapse(view);
    }
}
