package com.cinexaft.hypercontrol.hook.wechat

import android.util.Log; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import org.luckypray.dexkit.DexKitBridge; import org.luckypray.dexkit.query.FindMethod; import org.luckypray.dexkit.query.matchers.MethodMatcher
import org.luckypray.dexkit.result.MethodData; import java.lang.reflect.Method

/** Hides WeChat typing-status notifications. */
object WechatTyping {
    private const val TAG = "HyperControl.WeChat"
    private val ANCHORS = arrayOf("MicroMsg.SignallingComponent", "[doDirectSend] mChattingContext is null!")
    private var hooked: Method? = null
    @JvmStatic
    @Synchronized
    fun install(dex: DexKitBridge, loader: ClassLoader) {
        if (hooked != null) return
        try {
            val methods: List<MethodData> = dex.findMethod(FindMethod()
                .searchPackages("com.tencent.mm.ui.chatting.component")
                .matcher(MethodMatcher().usingEqStrings(*ANCHORS)))
            val candidates = methods.filter { it.returnTypeName == Void.TYPE.name }
            Log.i(TAG, "typing-status candidates: anchors=${methods.size}, void=${candidates.size}")
            if (candidates.size != 1) {
                Log.w(TAG, "Expected one typing-status method, found ${candidates.size}; skipping hook")
                return
            }
            val method = candidates.single().getMethodInstance(loader)
            if (method.returnType != Void.TYPE) {
                Log.w(TAG, "typing-status return type mismatch; skipping hook: $method")
                return
            }
            Reflect.hook(method) { chain -> if (State.enabled(20)) null else chain.proceed() }
            hooked = method
            Log.i(TAG, "typing-status suppressed: $method")
        } catch (error: Throwable) { Log.w(TAG, "typing-status setup failed", error) }
    }
}
