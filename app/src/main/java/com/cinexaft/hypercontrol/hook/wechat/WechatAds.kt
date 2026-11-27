package com.cinexaft.hypercontrol.hook.wechat

import android.util.Log; import android.view.View; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import org.luckypray.dexkit.DexKitBridge; import org.luckypray.dexkit.query.FindClass
import org.luckypray.dexkit.query.matchers.ClassMatcher; import org.luckypray.dexkit.result.ClassData; import java.lang.reflect.Method; import java.util.LinkedHashSet

/** Hides sponsored rows in the WeChat SNS feed. */
object WechatAds {
    private const val TAG = "HyperControl.WeChat"
    private const val ADAPTER = "com.tencent.mm.plugin.sns.ui.SnsTimeLineBaseAdapter"
    private const val SNS_INFO = "com.tencent.mm.plugin.sns.storage.SnsInfo"
    private val hooked: MutableSet<Method> = LinkedHashSet()
    private val hidden = java.util.WeakHashMap<View, Int>()
    @JvmStatic
    @Synchronized
    fun install(dex: DexKitBridge, loader: ClassLoader) {
        try {
            val adapters: List<ClassData> = dex.findClass(FindClass().matcher(ClassMatcher().usingEqStrings(ADAPTER)))
            for (adapter in adapters) {
                val type = adapter.getInstance(loader)
                for (method in type.declaredMethods) {
                    if (hooked.contains(method) || !isFeedBinder(method)) continue
                    try {
                        Reflect.hook(method) { chain ->
                            for (view in chain.args.filterIsInstance<View>()) {
                                hidden.remove(view)?.let { view.visibility = it }
                            }
                            val result = chain.proceed()
                            if (result !is View) return@hook result
                            hidden.remove(result)?.let { previous ->
                                if (result.visibility == View.GONE) result.visibility = previous
                            }
                            if (!State.enabled(21)) return@hook result
                            val info = findSnsInfo(chain.args)
                            if (info != null && isAd(info)) {
                                hidden[result] = result.visibility
                                result.visibility = View.GONE
                            }
                            result
                        }
                        hooked.add(method)
                        Log.i(TAG, "feed ad filter hooked: $method")
                    } catch (error: Throwable) { Log.w(TAG, "feed ad hook failed: $method", error) }
                }
            }
        } catch (error: Throwable) { Log.w(TAG, "ad filter setup failed", error) }
    }
    private fun isFeedBinder(method: Method): Boolean {
        if (method.returnType != View::class.java) return false
        var info = false
        var index = false
        var view = false
        for (type in method.parameterTypes) {
            info = info || SNS_INFO == type.name
            index = index || type == Int::class.javaPrimitiveType!!
            view = view || View::class.java.isAssignableFrom(type)
        }
        return info && index && view
    }
    private fun findSnsInfo(arguments: List<*>): Any? = arguments.firstOrNull {
        it != null && SNS_INFO == it.javaClass.name
    }
    private fun isAd(info: Any): Boolean = try {
        java.lang.Boolean.TRUE == info.javaClass.getMethod("isAd").invoke(info)
    } catch (error: ReflectiveOperationException) {
        Log.w(TAG, "cannot read isAd", error)
        false
    }
}
