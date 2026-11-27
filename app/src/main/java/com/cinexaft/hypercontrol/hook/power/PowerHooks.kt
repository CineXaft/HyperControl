package com.cinexaft.hypercontrol.hook.power

import android.content.Context; import com.cinexaft.hypercontrol.base.Entry.Bindings

object PowerHooks {
    @JvmStatic
    @Throws(ClassNotFoundException::class)
    fun install(loader: ClassLoader) {
        Bindings.hook(loader, "com.miui.powerkeeper.cloudcontrol.LocalUpdateUtils", "startCloudSyncData",
            Void.TYPE, Bindings.override(4, null), Context::class.java, Boolean::class.javaPrimitiveType!!)
        Bindings.hook(loader, "miui.process.ProcessManager", "kill", Boolean::class.javaPrimitiveType!!,
            Bindings.override(3, java.lang.Boolean.FALSE), loader.loadClass("miui.process.ProcessConfig"))
    }
}
