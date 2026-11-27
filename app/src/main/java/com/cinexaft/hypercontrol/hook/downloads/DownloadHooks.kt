package com.cinexaft.hypercontrol.hook.downloads

import android.content.Context; import com.cinexaft.hypercontrol.base.Entry.Bindings; import com.cinexaft.hypercontrol.base.Entry.State; import io.github.libxposed.api.XposedInterface; import java.io.File; import java.io.IOException

object DownloadHooks {
    @JvmStatic
    fun install(loader: ClassLoader) {
        val config = "com.android.providers.downloads.config.XLConfig"
        val disableDebug = Bindings.override(6, null)
        Bindings.hook(loader, config, "setDebug", Void.TYPE, disableDebug, Context::class.java, Boolean::class.javaPrimitiveType!!, String::class.java)
        Bindings.hook(loader, config, "setSoDebug", Void.TYPE, disableDebug, Boolean::class.javaPrimitiveType!!, String::class.java)
        Bindings.hook(loader, "com.android.providers.downloads.util.FileUtil", "createFile", File::class.java,
            XposedInterface.ExceptionMode.PASSTHROUGH, { chain ->
                val path = chain.getArg(0)
                if (State.enabled(6) && path is String && path.contains(".xlDownload")) {
                    throw IOException(".xlDownload is blocked")
                }
                chain.proceed()
            }, String::class.java)
    }
}
