package com.cinexaft.hypercontrol.base

import android.app.Activity; import android.content.Context; import android.content.pm.PackageManager.NameNotFoundException; import android.content.res.Resources; import android.os.Bundle; import androidx.fragment.app.FragmentActivity
import com.cinexaft.hypercontrol.base.Entry.Targets.SETTINGS; import com.cinexaft.hypercontrol.base.Entry.Targets.SYSTEM_UI

object PageBridge {
    private const val PAGE_TAG = "hypercontrol_native_page"
    @JvmStatic
    fun show(activity: Activity, arguments: Bundle?, title: CharSequence?) {
        val host = activity as FragmentActivity
        val args = Bundle(arguments ?: Bundle.EMPTY).apply {
            if (title != null && !containsKey(NativeScreen.TITLE_KEY))
                putString(NativeScreen.TITLE_KEY, title.toString())
        }
        val container = host.resources.getIdentifier("main_content", "id", SETTINGS)
        check(container != 0) { "Settings main_content container not found" }
        val fragments = host.supportFragmentManager
        val current = fragments.findFragmentById(container)
        if (current is NativeScreen && current.tag == PAGE_TAG && current.isAdded && !current.isHidden
            && target(current.arguments) == target(args)
            && current.arguments?.getString(NativeScreen.TITLE_KEY) == args.getString(NativeScreen.TITLE_KEY)) return
        fragments.beginTransaction()
            .replace(container, NativeScreen().apply { this.arguments = args }, PAGE_TAG)
            .commitNowAllowingStateLoss()
    }
    private fun target(args: Bundle?) = args?.getString("target")
        ?: SYSTEM_UI.takeIf { args?.getBoolean("hc_systemui") == true }
    object Strings {
        const val MODULE = "com.cinexaft.hypercontrol"
        @JvmStatic
        fun appLabel(context: Context, packageName: String): String = try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (_: NameNotFoundException) { packageName }
        fun resources(host: Context): Resources? = try {
            host.createPackageContext(MODULE, 0)
                .createConfigurationContext(host.resources.configuration).resources
        } catch (_: NameNotFoundException) { null }
        fun get(resources: Resources?, id: Int): String = try {
            resources?.getString(id).orEmpty()
        } catch (_: Resources.NotFoundException) { "" }
        @JvmStatic
        fun get(host: Context, id: Int) = get(resources(host), id)
    }
}
