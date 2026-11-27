package com.cinexaft.hypercontrol.hook.systemui

import android.view.View; import android.view.ViewGroup; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import io.github.libxposed.api.XposedInterface

class SignalHook : XposedInterface.Hooker, View.OnLayoutChangeListener {
    override fun intercept(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        val signalMask = State.signalMask()
        if (signalMask != 0) {
            try {
                val view = chain.thisObject as View
                val cls = view.javaClass
                when {
                    cls == mobileViewClass -> if ((signalMask and 1) != 0) {
                        child(view, "mobile_left_mobile_inout", 0)?.let {
                            collapse(it)
                            it.addOnLayoutChangeListener(this)
                        }
                    }
                    cls == wifiViewClass -> {
                        transparent(view, "wifi_activity", signalMask, 2, 1)
                        transparent(view, "wifi_standard", signalMask, 4, 2)
                    }
                }
            } catch (error: Throwable) { Reflect.log(error) }
        }
        return result
    }
    override fun onLayoutChange(view: View, left: Int, top: Int, right: Int, bottom: Int,
        oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) = collapse(view)
    companion object {
        private val IDS = IntArray(3)
        private var mobileViewClass: Class<*>? = null
        private var wifiViewClass: Class<*>? = null
        @JvmStatic
        fun install(classLoader: ClassLoader) {
            try {
                mobileViewClass = loadClass(classLoader, "com.android.systemui.statusbar.pipeline.mobile.ui.view.ModernStatusBarMobileView")
                wifiViewClass = loadClass(classLoader, "com.android.systemui.statusbar.pipeline.wifi.ui.view.ModernStatusBarWifiView")
                Reflect.hook(classLoader.loadClass("com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView")
                    .getDeclaredMethod("initView", String::class.java, classLoader.loadClass("kotlin.jvm.functions.Function0")), SignalHook())
            } catch (error: Throwable) { Reflect.log(error) }
        }
        private fun loadClass(classLoader: ClassLoader, name: String): Class<*>? = try { classLoader.loadClass(name) }
            catch (_: ClassNotFoundException) { null }
        private fun child(view: View, name: String, slot: Int): View? {
            val id = SystemUiHooks.id(view, name, IDS, slot)
            return if (id > 0) view.findViewById(id) else null
        }
        private fun collapse(view: View) {
            val layoutParams = view.layoutParams ?: return
            if ((layoutParams.width or layoutParams.height) == 0) return
            layoutParams.width = 0
            layoutParams.height = 0
            view.layoutParams = layoutParams
        }
        private fun transparent(view: View, name: String, mask: Int, bit: Int, slot: Int) {
            if ((mask and bit) == 0) return
            child(view, name, slot)?.alpha = 0.0f
        }
    }
}
