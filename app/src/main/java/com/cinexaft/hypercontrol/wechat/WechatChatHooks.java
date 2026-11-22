package com.cinexaft.hypercontrol.wechat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import com.cinexaft.hypercontrol.Dex;
import com.cinexaft.hypercontrol.Entry;
import com.cinexaft.hypercontrol.Hooks;
import com.cinexaft.hypercontrol.Reflect;
import com.cinexaft.hypercontrol.State;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

/** Three WeChat chat helpers imported from the supplied WAuxiliary hook sources. */
public final class WechatChatHooks {
    private static final String TAG = "HyperControl.WeChat";
    private static boolean installed;

    private WechatChatHooks() {}

    public static synchronized void install(Context context, ClassLoader loader) {
        if (installed) return;
        installed = true;
        if (State.enabled(18)) installSelectOriginal(loader);
        if (!State.enabled(20)) return;
        DexKitBridge bridge = Dex.of(context, loader);
        if (bridge != null) installNoTyping(bridge, loader);
    }

    private static void installSelectOriginal(ClassLoader loader) {
        hookGalleryCreate(loader, "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI");
        hookGalleryCreate(loader, "com.tencent.mm.plugin.gallery.ui.ImagePreviewUI");
    }

    /** Set the flags on the actual gallery Activity intent before its onCreate reads them. */
    private static void hookGalleryCreate(ClassLoader loader, String className) {
        try {
            Class<?> type = Class.forName(className, false, loader);
            Method onCreate = type.getDeclaredMethod("onCreate", Bundle.class);
            Entry.module.hook(onCreate).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        if (State.enabled(18)) {
                            Object activity = chain.getThisObject();
                            if (activity instanceof Activity) {
                                Intent intent = ((Activity) activity).getIntent();
                                if (intent != null) {
                                    intent.putExtra("send_raw_img", true);
                                    intent.putExtra("key_send_raw_image", true);
                                }
                            }
                        }
                        return chain.proceed();
                    });
            Log.i(TAG, "auto-select original hooked: " + className + "#onCreate");
        } catch (Throwable error) {
            Log.w(TAG, "gallery Activity hook unavailable: " + className, error);
        }
    }

    private static void installNoTyping(DexKitBridge bridge, ClassLoader loader) {
        try {
            MethodData data = bridge.findMethod(new FindMethod().searchPackages("com.tencent.mm.ui.chatting.component")
                    .matcher(new MethodMatcher().usingEqStrings(
                            "MicroMsg.SignallingComponent", "[doDirectSend] mChattingContext is null!")))
                    .firstOrNull();
            if (data == null || !"void".equals(data.getReturnTypeName())) {
                Log.w(TAG, "typing-status method not found; switch inactive");
                return;
            }
            Method target = data.getMethodInstance(loader);
            Entry.module.hook(target).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> State.enabled(20) ? null : chain.proceed());
            Log.i(TAG, "typing-status signal suppressed at " + target);
        } catch (Throwable error) {
            Log.w(TAG, "typing-status hook unavailable", error);
        }
    }
}
