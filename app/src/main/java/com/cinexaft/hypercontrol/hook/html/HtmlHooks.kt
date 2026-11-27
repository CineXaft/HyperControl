package com.cinexaft.hypercontrol.hook.html

import com.cinexaft.hypercontrol.base.Entry.Bindings

object HtmlHooks {
    @JvmStatic
    fun install(loader: ClassLoader) {
        Bindings.hook(loader, "com.android.settings.cloud.JobTask", "updateCloudAllData",
            Void.TYPE, Bindings.override(5, null))
    }
}
