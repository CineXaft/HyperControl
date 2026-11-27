package com.cinexaft.hypercontrol.hook.systemui

import android.util.Log; import android.view.View; import com.cinexaft.hypercontrol.base.Entry.Bindings; import com.cinexaft.hypercontrol.base.Entry.State

object SystemUiHooks {
    @JvmStatic
    fun id(view: View, name: String, cache: IntArray, slot: Int): Int {
        var id = cache[slot]
        if (id == 0) {
            id = view.resources.getIdentifier(name, "id", "com.android.systemui")
            cache[slot] = if (id == 0) -1 else id
        }
        return id
    }
    @JvmStatic
    fun install(loader: ClassLoader) {
        installFeature("signal indicators") { SignalHook.install(loader) }
        installFeature("mobile type on Wi-Fi") { MobileWifiHook.install(loader) }
        installFeature("mobile/Wi-Fi order") { SwapHook.install(loader) }
        installFeature("media island") {
            Bindings.hook(loader, "com.android.systemui.statusbar.notification.mediaisland.MiuiIslandMediaControllerImpl\$mediaDataChangeListener\$1",
                "onMediaDataChanged", Void.TYPE, Bindings.override(0, null),
                loader.loadClass("com.android.systemui.media.controls.shared.model.MediaData"))
        }
        installFeature("mobile type text") {
            Bindings.hook(loader, "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl",
                "getMobileTypeName", String::class.java, { chain ->
                    val text = State.customMobileText()
                    text ?: chain.proceed()
                }, Int::class.javaPrimitiveType!!)
        }
    }
    private inline fun installFeature(feature: String, block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            Log.w("HyperControl.SystemUI", "$feature hook initialization failed", error)
        }
    }
}
