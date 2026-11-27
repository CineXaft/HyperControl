package com.cinexaft.hypercontrol.hook.systemui

import android.util.Log; import android.view.View; import android.view.ViewGroup; import com.cinexaft.hypercontrol.base.Entry.Hooks; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect
import io.github.libxposed.api.XposedInterface

class SwapHook : XposedInterface.Hooker {
    override fun intercept(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        if (!State.enabled(12)) return result
        val host = chain.thisObject
        if (host !is ViewGroup || !container!!.isInstance(host)) return result
        host.post {
            try { order(host) } catch (error: Throwable) { Reflect.log(error) }
        }
        return result
    }
    companion object {
        private const val CONTAINER = "com.android.systemui.statusbar.views.MiuiStatusIconContainer"
        private const val WIFI = "wifi_combo"
        private const val MOBILE = "mobile_combo"
        private val ids = IntArray(2)
        private var container: Class<*>? = null
        @JvmStatic
        fun install(classLoader: ClassLoader) {
            try { container = classLoader.loadClass(CONTAINER) }
            catch (error: Throwable) {
                Log.w(Hooks.TAG, "no icon container, icon swap unavailable")
                return
            }
            Hooks.install(Hooks.byNameCount(container!!, "onViewAdded", 1), SwapHook())
        }
        private fun order(parent: ViewGroup) {
            val wifiId = SystemUiHooks.id(parent, WIFI, ids, 0)
            val mobileId = SystemUiHooks.id(parent, MOBILE, ids, 1)
            if (wifiId <= 0 || mobileId <= 0) return
            var wifi = -1
            var mobile = -1
            for (i in 0 until parent.childCount) {
                val id = parent.getChildAt(i).id
                if (wifi < 0 && id == wifiId) wifi = i
                if (mobile < 0 && id == mobileId) mobile = i
                if (wifi >= 0 && mobile >= 0) break
            }
            if (wifi < 0 || mobile < 0 || wifi < mobile) return
            val wifiView = parent.getChildAt(wifi)
            val mobileView = parent.getChildAt(mobile)
            parent.removeView(wifiView)
            parent.removeView(mobileView)
            parent.addView(wifiView, mobile)
            parent.addView(mobileView, wifi)
            Log.i(Hooks.TAG, "swapped wifi and mobile icons")
        }
    }
}
