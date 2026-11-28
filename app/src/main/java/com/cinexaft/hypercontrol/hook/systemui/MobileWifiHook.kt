package com.cinexaft.hypercontrol.hook.systemui

import com.cinexaft.hypercontrol.base.Entry
import com.cinexaft.hypercontrol.base.Entry.State
import com.cinexaft.hypercontrol.base.SettingsHook.Reflect
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Collections
import java.util.WeakHashMap

class MobileWifiHook : XposedInterface.Hooker, InvocationHandler {
    override fun intercept(chain: XposedInterface.Chain): Any? {
        val result = chain.proceed()
        if (State.enabled(10)) {
            try {
                val model = chain.thisObject
                val value = wifi?.get(model) ?: wifiSources[model]
                    ?: throw IllegalStateException("Wi-Fi flow missing from cellular view model")
                return combine(result, value)
            } catch (error: Throwable) { Reflect.log(error) }
        }
        return result
    }

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? = when (method.name) {
        "invoke" -> (args!![0] as Boolean) && !(args[1] as Boolean)
        "toString" -> "HyperControl.MobileTypeWifiGate"
        "hashCode" -> System.identityHashCode(proxy)
        else -> proxy === args?.getOrNull(0)
    }

    companion object {
        private const val COMBINED_FLOW =
            "kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$\$inlined\$unsafeFlow\$1"
        private const val COMBINED_FLOW_ALT =
            "kotlinx.coroutines.flow.FlowKt__ZipKt\$combine\$\$inlined\$combineUnsafe\$FlowKt__ZipKt\$1"
        private var combineMethod: Method? = null
        private var combineArrayMethod: Method? = null
        private var combineConstructor: Constructor<*>? = null
        private var flowType: Class<*>? = null
        private var reducer: Any? = null
        private var wifi: Field? = null
        private val wifiSources = Collections.synchronizedMap(WeakHashMap<Any, Any>())

        @JvmStatic
        fun install(classLoader: ClassLoader) {
            try {
                val flow = classLoader.loadClass("kotlinx.coroutines.flow.Flow")
                val function3 = classLoader.loadClass("kotlin.jvm.functions.Function3")
                flowType = flow
                resolveCombiner(classLoader, flow, function3)
                val hook = MobileWifiHook()
                reducer = Proxy.newProxyInstance(classLoader, arrayOf(function3), hook)
                val viewModel = classLoader.loadClass(
                    "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiCellularIconVM"
                )
                wifi = try { findField(viewModel, "wifiAvailable").apply { isAccessible = true } }
                    catch (_: NoSuchFieldException) { null }
                viewModel.declaredConstructors.forEach { constructor ->
                    val index = constructor.parameterTypes.indexOfFirst {
                        it.name == "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractor"
                    }
                    if (index >= 0) Reflect.attach(constructor, { chain ->
                        val result = chain.proceed()
                        if (State.enabled(10)) {
                            val interactor = chain.getArg(index) as Any
                            val source = findField(interactor.javaClass, "wifiAvailable")
                                .apply { isAccessible = true }.get(interactor)
                                ?: throw IllegalStateException("Wi-Fi flow missing from mobile interactor")
                            wifiSources[chain.thisObject!!] = source
                        }
                        result
                    }, XposedInterface.ExceptionMode.PROTECTIVE)
                }
                var hooked = false
                arrayOf("getMobileTypeVisible", "getMobileTypeSingleVisible").forEach { name ->
                    try {
                        val method = viewModel.getDeclaredMethod(name)
                        Entry.module.deoptimize(method)
                        Reflect.hook(method, hook)
                        hooked = true
                    } catch (_: NoSuchMethodException) {
                        // This SystemUI version has only one of the two displays.
                    }
                }
                if (!hooked) throw NoSuchMethodException("mobile type visibility method not found")
            } catch (error: Throwable) { Reflect.log(error) }
        }

        private fun resolveCombiner(classLoader: ClassLoader, flow: Class<*>, function3: Class<*>) {
            val flowKt = classLoader.loadClass("kotlinx.coroutines.flow.FlowKt")
            combineMethod = arrayOf("flowCombine", "combine").asSequence()
                .mapNotNull { name ->
                    try {
                        flowKt.getDeclaredMethod(name, flow, flow, function3).apply { isAccessible = true }
                    } catch (_: NoSuchMethodException) { null }
                }.firstOrNull()
            if (combineMethod != null) return

            combineArrayMethod = flowKt.declaredMethods.firstOrNull { method ->
                val params = method.parameterTypes
                method.name == "combine" && params.size == 2 && params[0].isArray &&
                    params[0].componentType == flow && params[1] == function3
            }?.apply { isAccessible = true }
            if (combineArrayMethod != null) return

            combineConstructor = arrayOf(COMBINED_FLOW, COMBINED_FLOW_ALT).asSequence()
                .mapNotNull { name ->
                    try {
                        classLoader.loadClass(name).declaredConstructors.firstOrNull { constructor ->
                            val params = constructor.parameterTypes
                            params.size == 3 && params[0] == flow && params[1] == flow && params[2] == function3
                        }?.apply { isAccessible = true }
                    } catch (_: Throwable) { null }
                }.firstOrNull()
            if (combineConstructor == null) {
                throw NoSuchMethodException("no compatible two-flow combine implementation")
            }
        }

        private fun findField(owner: Class<*>, name: String): Field {
            var type: Class<*>? = owner
            while (type != null) {
                try { return type.getDeclaredField(name) }
                catch (_: NoSuchFieldException) { type = type.superclass }
            }
            throw NoSuchFieldException("$name in ${owner.name}")
        }

        private fun combine(first: Any?, second: Any?): Any? {
            combineMethod?.let { return it.invoke(null, first, second, reducer) }
            combineArrayMethod?.let {
                val flows = ReflectArray.newInstance(flowType!!, 2)
                ReflectArray.set(flows, 0, first)
                ReflectArray.set(flows, 1, second)
                return it.invoke(null, flows, reducer)
            }
            return combineConstructor!!.newInstance(first, second, reducer)
        }
    }
}
