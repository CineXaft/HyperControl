package com.cinexaft.hypercontrol;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/* JADX INFO: loaded from: classes.dex */
public final class MobileWifiHook implements XposedInterface.Hooker, InvocationHandler {
    private static Method combine;
    private static Object reducer;
    private static Field wifi;

    public static void install(ClassLoader classLoader) {
        try {
            Class<?> clsLoadClass = classLoader.loadClass("kotlinx.coroutines.flow.Flow");
            Class<?> clsLoadClass2 = classLoader.loadClass("kotlin.jvm.functions.Function3");
            combine = classLoader.loadClass("kotlinx.coroutines.flow.FlowKt").getDeclaredMethod("flowCombine", clsLoadClass, clsLoadClass, clsLoadClass2);
            MobileWifiHook mobileWifiHook = new MobileWifiHook();
            reducer = Proxy.newProxyInstance(classLoader, new Class[]{clsLoadClass2}, mobileWifiHook);
            Class<?> clsLoadClass3 = classLoader.loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM");
            Field declaredField = clsLoadClass3.getDeclaredField("wifiAvailable");
            declaredField.setAccessible(true);
            wifi = declaredField;
            Reflect.hook(clsLoadClass3.getDeclaredMethod("getMobileTypeVisible", new Class[0]), mobileWifiHook);
        } catch (Throwable th) {
            Reflect.log(th);
        }
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object objProceed = chain.proceed();
        if (State.enabled(10)) {
            try {
                Object obj = wifi.get(chain.getThisObject());
                if (obj != null) {
                    return combine.invoke(null, objProceed, obj, reducer);
                }
            } catch (Throwable th) {
                Reflect.log(th);
                return objProceed;
            }
        }
        return objProceed;
    }

    @Override // java.lang.reflect.InvocationHandler
    public Object invoke(Object obj, Method method, Object[] objArr) {
        String name = method.getName();
        if ("invoke".equals(name)) {
            return (!((Boolean) objArr[0]).booleanValue() || ((Boolean) objArr[1]).booleanValue()) ? Boolean.FALSE : Boolean.TRUE;
        }
        if ("toString".equals(name)) {
            return "HyperControl.MobileTypeWifiGate";
        }
        if ("hashCode".equals(name)) {
            return Integer.valueOf(System.identityHashCode(obj));
        }
        return Boolean.valueOf(obj == objArr[0]);
    }
}
