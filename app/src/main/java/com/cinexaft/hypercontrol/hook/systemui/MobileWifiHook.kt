package com.cinexaft.hypercontrol.hook.systemui

import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import io.github.libxposed.api.XposedInterface; import java.lang.reflect.Field; import java.lang.reflect.InvocationHandler; import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MobileWifiHook : XposedInterface.Hooker, InvocationHandler {
    override fun intercept(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        if (State.enabled(10)) {
            try {
                val value = wifi!!.get(chain.thisObject)
                if (value != null) return combine!!.invoke(null, result, value, reducer)
            } catch (error: Throwable) { Reflect.log(error) }
        }
        return result
    }
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
        return when (method.name) {
            "invoke" -> (args!![0] as Boolean) && !(args[1] as Boolean)
            "toString" -> "HyperControl.MobileTypeWifiGate"
            "hashCode" -> System.identityHashCode(proxy)
            else -> proxy === args?.getOrNull(0)
        }
    }
    companion object {
        private var combine: Method? = null
        private var reducer: Any? = null
        private var wifi: Field? = null
        @JvmStatic
        fun install(classLoader: ClassLoader) {
            try {
                val flow = classLoader.loadClass("kotlinx.coroutines.flow.Flow")
                val function3 = classLoader.loadClass("kotlin.jvm.functions.Function3")
                combine = classLoader.loadClass("kotlinx.coroutines.flow.FlowKt").getDeclaredMethod("flowCombine", flow, flow, function3)
                val hook = MobileWifiHook()
                reducer = Proxy.newProxyInstance(classLoader, arrayOf(function3), hook)
                val viewModel = classLoader.loadClass("com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM")
                val field = viewModel.getDeclaredField("wifiAvailable")
                field.isAccessible = true
                wifi = field
                Reflect.hook(viewModel.getDeclaredMethod("getMobileTypeVisible"), hook)
            } catch (error: Throwable) { Reflect.log(error) }
        }
    }
}
