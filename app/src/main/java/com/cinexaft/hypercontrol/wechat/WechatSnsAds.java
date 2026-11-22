package com.cinexaft.hypercontrol.wechat;

import android.content.Context;
import android.util.Log;
import android.view.View;
import com.cinexaft.hypercontrol.Dex;
import com.cinexaft.hypercontrol.Entry;
import com.cinexaft.hypercontrol.Hooks;
import com.cinexaft.hypercontrol.Reflect;
import com.cinexaft.hypercontrol.State;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.List;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

/** Hides sponsored Moments feed rows at the timeline adapter's model-to-view boundary. */
public final class WechatSnsAds {
    private static final String TAG = "HyperControl.WeChatAds";
    private static final String ADAPTER_MARKER = "com.tencent.mm.plugin.sns.ui.SnsTimeLineBaseAdapter";
    private static final String SNS_INFO = "com.tencent.mm.plugin.sns.storage.SnsInfo";
    private static boolean installed;

    private WechatSnsAds() {}

    public static synchronized void install(Context context, ClassLoader loader) {
        if (installed || !State.enabled(21)) return;
        installed = true;
        DexKitBridge bridge = Dex.of(context, loader);
        if (bridge == null) return;
        int hooks = 0;
        try {
            List<ClassData> candidates = bridge.findClass(new FindClass()
                    .matcher(new ClassMatcher().usingStrings(ADAPTER_MARKER)));
            for (ClassData candidate : candidates) {
                for (MethodData methodData : candidate.getMethods()) {
                    Method method;
                    try { method = methodData.getMethodInstance(loader); }
                    catch (Throwable ignored) { continue; }
                    if (!isFeedBinder(method)) continue;
                    Entry.module.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                            .intercept(chain -> {
                                Object result = chain.proceed();
                                if (!(result instanceof View) || !State.enabled(21)) return result;
                                Object item = findSnsInfo(chain.getArgs());
                                if (item == null) return result;
                                boolean ad = isAd(item);
                                ((View) result).setVisibility(ad ? View.GONE : View.VISIBLE);
                                return result;
                            });
                    Log.i(TAG, "hooked timeline row binder: " + method);
                    hooks++;
                }
            }
        } catch (Throwable error) {
            Reflect.log(error);
        }
        if (hooks == 0) Log.w(TAG, "timeline row binder not found; Moments ad filter inactive");
    }

    private static boolean isFeedBinder(Method method) {
        if (method.getReturnType() != View.class) return false;
        Class<?>[] params = method.getParameterTypes();
        boolean hasSns = false;
        boolean hasInt = false;
        boolean hasView = false;
        for (Class<?> type : params) {
            if (SNS_INFO.equals(type.getName())) hasSns = true;
            if (type == int.class) hasInt = true;
            if (View.class.isAssignableFrom(type)) hasView = true;
        }
        return hasSns && hasInt && hasView;
    }

    private static Object findSnsInfo(List<?> args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg != null && SNS_INFO.equals(arg.getClass().getName())) return arg;
        }
        return null;
    }

    private static boolean isAd(Object info) {
        try {
            Method method = info.getClass().getMethod("isAd");
            Object result = method.invoke(info);
            return Boolean.TRUE.equals(result);
        } catch (Throwable error) {
            Log.w(TAG, "cannot read SnsInfo.isAd", error);
            return false;
        }
    }
}
