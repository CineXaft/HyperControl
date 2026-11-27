package com.cinexaft.hypercontrol.hook.wechat

import android.app.Activity; import android.content.Intent; import android.os.Bundle; import android.util.Log; import com.cinexaft.hypercontrol.base.Entry; import com.cinexaft.hypercontrol.base.Entry.State
import io.github.libxposed.api.XposedInterface; import java.lang.reflect.Method

/** Sets WeChat's native send-raw-image flag at both gallery-launch paths. */
object WechatOriginalImage {
    private const val TAG = "HyperControl.WeChat"
    private const val ALBUM_PREVIEW = "com.tencent.mm.plugin.gallery.ui.AlbumPreviewUI"
    private const val IMAGE_PREVIEW = "com.tencent.mm.plugin.gallery.ui.ImagePreviewUI"
    private const val PICKER = "com.tencent.mm.ui.chatting.h4"
    private val galleryClasses = setOf(ALBUM_PREVIEW, IMAGE_PREVIEW)
    private val installed = HashSet<Method>()
    @JvmStatic
    @Synchronized
    fun install(loader: ClassLoader) {
        val module = Entry.module ?: return
        hookGalleryPicker(module, loader)
        hookActivityLaunches(module)
    }
    /** Newer WeChat picker passes its launch Intent as the last of eight arguments. */
    private fun hookGalleryPicker(module: io.github.libxposed.api.XposedModule, loader: ClassLoader) {
        try {
            val owner = loader.loadClass(PICKER)
            val intType = Int::class.javaPrimitiveType ?: Integer.TYPE
            val method = owner.getDeclaredMethod(
                "Y", Boolean::class.javaPrimitiveType, intType, intType, intType,
                intType, String::class.java, String::class.java, Intent::class.java
            )
            if (method.returnType != Void.TYPE) return
            hookOnce(module, method) { chain ->
                val args = chain.args.toTypedArray()
                if (State.enabled(18) && args.size == 8
                    && (args[7] == null || args[7] is Intent)) {
                    val intent = (args[7] as? Intent)?.let(::Intent) ?: Intent()
                    args[7] = intent.putExtra("send_raw_img", true)
                }
                chain.proceed(args)
            }
        } catch (error: ReflectiveOperationException) {
            Log.i(TAG, "Native gallery picker signature not present; using Activity launch hooks")
        }
    }
    /** Mark gallery destination Intents before WeChat reads its picker flags. */
    private fun hookActivityLaunches(module: io.github.libxposed.api.XposedModule) {
        val intType = Int::class.javaPrimitiveType ?: Integer.TYPE
        hookActivityMethod(module, "startActivity", arrayOf(Intent::class.java))
        hookActivityMethod(module, "startActivity", arrayOf(Intent::class.java, Bundle::class.java))
        hookActivityMethod(module, "startActivityForResult", arrayOf(Intent::class.java, intType))
        hookActivityMethod(module, "startActivityForResult", arrayOf(Intent::class.java, intType, Bundle::class.java))
    }
    private fun hookActivityMethod(
        module: io.github.libxposed.api.XposedModule,
        name: String,
        parameters: Array<Class<*>>
    ) {
        try {
            hookOnce(module, Activity::class.java.getDeclaredMethod(name, *parameters)) { chain ->
                if (State.enabled(18) && chain.thisObject is Activity) {
                    val intent = chain.getArg(0) as? Intent
                    if (intent != null && intent.component?.className in galleryClasses) {
                        intent.putExtra("send_raw_img", true)
                    }
                }
                chain.proceed()
            }
        } catch (error: ReflectiveOperationException) {
            Log.w(TAG, "Activity.$name${parameters.contentToString()} unavailable", error)
        }
    }
    private fun hookOnce(
        module: io.github.libxposed.api.XposedModule,
        method: Method,
        hooker: XposedInterface.Hooker
    ) {
        if (!installed.add(method)) return
        try {
            method.isAccessible = true
            module.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(hooker)
            Log.i(TAG, "Original-image hook installed: $method")
        } catch (error: Throwable) {
            installed.remove(method)
            Log.w(TAG, "Original-image hook failed: $method", error)
        }
    }
}
