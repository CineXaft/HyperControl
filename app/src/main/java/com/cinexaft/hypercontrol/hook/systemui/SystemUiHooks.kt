package com.cinexaft.hypercontrol.hook.systemui

import android.util.Log
import android.view.View
import com.cinexaft.hypercontrol.base.Entry.Bindings
import com.cinexaft.hypercontrol.base.Entry.Hooks
import com.cinexaft.hypercontrol.base.Entry.State
import com.cinexaft.hypercontrol.base.SettingsHook.Reflect

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
        installFeature("MIUI classic lock screen clock") { LockscreenClockHook.installClassic(loader) }
        installFeature("dynamic lock screen clock size") { LockscreenClockHook.installDynamicSize(loader) }
        installFeature("media island") {
            val mediaData = loader.loadClass("com.android.systemui.media.controls.shared.model.MediaData")
            val listeners = arrayOf("MiuiIslandMediaController", "MiuiIslandMediaControllerImpl").mapNotNull { controller ->
                Hooks.bySignature(loader,
                    "com.android.systemui.statusbar.notification.mediaisland.$controller\$mediaDataChangeListener\$1",
                    "onMediaDataChanged", arrayOf(mediaData), Void.TYPE)
            }
            if (listeners.isEmpty()) Log.w("HyperControl.SystemUI", "Media island callback not found")
            val hooker = Bindings.override(0, null)
            listeners.forEach { Reflect.hook(it, hooker) }
        }
        installFeature("mobile type text") {
            val owner = "com.android.systemui.statusbar.pipeline.mobile.domain.interactor.MiuiMobileIconInteractorImpl"
            val intType = Int::class.javaPrimitiveType!!
            val signatures = arrayOf(
                arrayOf<Class<*>>(intType),
                arrayOf<Class<*>>(intType, Array<String>::class.java)
            )
            val methods = signatures.mapNotNull { params ->
                Hooks.bySignature(loader, owner, "getMobileTypeName", params, String::class.java)
            }
            if (methods.isEmpty()) Log.w("HyperControl.SystemUI", "mobile type text signature not found")
            methods.forEach { method ->
                Hooks.install(method) { chain -> State.customMobileText() ?: chain.proceed() }
            }
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
