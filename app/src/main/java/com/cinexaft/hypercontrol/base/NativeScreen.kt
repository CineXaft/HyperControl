package com.cinexaft.hypercontrol.base

import android.app.Activity; import android.content.ActivityNotFoundException; import android.content.Context; import android.content.Intent; import android.content.pm.ApplicationInfo; import android.content.pm.PackageManager
import android.graphics.Bitmap; import android.graphics.Canvas; import android.graphics.drawable.BitmapDrawable; import android.net.Uri; import android.os.Bundle; import android.os.Handler
import android.os.Looper; import android.provider.Settings; import android.util.Log; import android.util.TypedValue; import android.widget.ImageView; import android.widget.Toast; import androidx.preference.Preference as AndroidPreference; import androidx.preference.PreferenceGroup; import androidx.preference.PreferenceManager; import androidx.preference.PreferenceScreen
import com.android.settings.SettingsPreferenceFragment; import com.android.settingslib.miuisettings.preference.IconPreference; import com.android.settingslib.miuisettings.preference.Preference; import com.android.settingslib.miuisettings.preference.PreferenceCategory; import com.android.settingslib.miuisettings.preference.SwitchPreference; import com.cinexaft.hypercontrol.R
import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.Entry.Targets.*; import com.cinexaft.hypercontrol.base.PageBridge.Strings; import com.cinexaft.hypercontrol.ui.AppPages; import com.cinexaft.hypercontrol.ui.ShortcutPages; import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean; import miuix.appcompat.app.ActionBar; import miuix.appcompat.app.ActionBarTransitionListener; import miuix.appcompat.app.AlertDialog; import miuix.appcompat.app.AppCompatActivity
import miuix.appcompat.internal.util.FloatingEffectHelper; import miuix.core.util.MiuixUIUtils

class NativeScreen : SettingsPreferenceFragment() {
    private var target: String? = null
    private var restartActionBar: ActionBar? = null
    private var restartButton: ImageView? = null
    private var restartTransition: ActionBarTransitionListener? = null

    override fun onResume() {
        super.onResume()
        val page = target ?: return
        if (!canRestart(page)) return
        val host = activity as? AppCompatActivity ?: return
        val actionBar = host.getAppCompatActionBar() ?: return
        if (restartActionBar === actionBar && restartButton != null) return
        val action = when (page) {
            FRAMEWORK -> R.string.hc_reboot
            SYSTEM_UI -> R.string.hc_restart
            else -> R.string.hc_stop
        }
        val label = Strings.get(host, action).let {
            if (page == FRAMEWORK) it else "$it ${preferenceScreen?.title}"
        }
        val button = ImageView(host).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val refreshAttr = host.resources.getIdentifier("actionBarRefreshIcon", "attr", SETTINGS)
            val refreshIcon = TypedValue()
            if (refreshAttr != 0 && host.theme.resolveAttribute(refreshAttr, refreshIcon, true)
                && refreshIcon.resourceId != 0) {
                setImageDrawable(host.getDrawable(refreshIcon.resourceId))
            } else {
                setImageResource(android.R.drawable.ic_popup_sync)
            }
            val iconSize = host.resources.getIdentifier("miuix_theme_title_button_width", "dimen", SETTINGS)
                .takeIf { it != 0 }?.let(host.resources::getDimensionPixelSize)
                ?: (40 * host.resources.displayMetrics.density + 0.5f).toInt()
            minimumWidth = iconSize
            minimumHeight = iconSize
            contentDescription = label
            isClickable = true
            isFocusable = true
            setOnClickListener { Restart.request(host, page) }
        }
        actionBar.setEndView(button)
        restartActionBar = actionBar
        restartButton = button
        val transition = object : ActionBarTransitionListener {
            override fun onActionBarMove(from: Float, to: Float) = Unit
            override fun onActionBarResizing(state: Int, progress: Float, height: Int) = Unit
            override fun onExpandStateChanged(state: Int) = Unit
            override fun onOverlayMaskStateChanged(overlayMask: Boolean) {
                FloatingEffectHelper.updateViewFloatingState(
                    button, !overlayMask, MiuixUIUtils.isLightTheme(host), true
                )
            }
        }
        restartTransition = transition
        actionBar.addActionBarTransitionListener(transition)
    }

    override fun onDestroyView() {
        restartTransition?.let { restartActionBar?.removeActionBarTransitionListener(it) }
        restartTransition = null
        restartActionBar?.setEndView(null)
        restartButton = null
        restartActionBar = null
        super.onDestroyView()
    }

    override fun getMetricsCategory() = 0
    override fun onCreatePreferences(savedState: Bundle?, rootKey: String?) {
        val args = arguments ?: Bundle.EMPTY
        target = args.getString("target") ?: SYSTEM_UI.takeIf { args.getBoolean("hc_systemui") }
        preferenceScreen = Pages.create(preferenceManager, target, args.getString(TITLE_KEY))
    }
    override fun onPreferenceTreeClick(screen: PreferenceScreen, preference: AndroidPreference): Boolean {
        val intent = preference.intent ?: return super.onPreferenceTreeClick(screen, preference)
        try {
            startActivity(intent)
        } catch (error: RuntimeException) {
            if (error !is ActivityNotFoundException && error !is SecurityException) throw error
            error.printStackTrace()
            context?.let { Toast.makeText(it, Strings.get(it, R.string.hc_unavailable), Toast.LENGTH_LONG).show() }
        }
        return true
    }
    override fun onStart() {
        super.onStart()
        activity?.title = preferenceScreen?.title
    }
    class Page internal constructor(private val screen: PreferenceScreen) {
        @JvmField val context: Context = screen.context
        private val resources = Strings.resources(context)
        private var group: PreferenceGroup = screen
        internal val iconStyle by lazy(LazyThreadSafetyMode.NONE) { IconStyle(context) }
        fun text(id: Int) = Strings.get(resources, id)
        fun group(title: CharSequence?) = apply {
            group = PreferenceCategory(context).also { category ->
                if (title != null) category.title = title
                screen.addPreference(category)
            }
        }
        fun group(titleRes: Int) = group(text(titleRes))
        fun feature(id: Int, titleRes: Int, summaryRes: Int) = option(State.key(id), titleRes, summaryRes)
        fun option(setting: String, titleRes: Int, summaryRes: Int): Page {
            val settingKey = State.setting(setting)
            val resolver = context.contentResolver
            return add(SwitchPreference(context).apply {
                key = setting
                isPersistent = false
                val readable = try {
                    isChecked = Settings.Global.getInt(resolver, settingKey, 0) == 1
                    true
                } catch (error: Throwable) {
                    Log.w("HyperControl", "Cannot read $settingKey", error)
                    false
                }
                isEnabled = readable
                setOnPreferenceChangeListener { _, value ->
                    try {
                        if (Settings.Global.putInt(resolver, settingKey, if (value as Boolean) 1 else 0)) {
                            return@setOnPreferenceChangeListener true
                        }
                    } catch (error: Throwable) {
                        error.printStackTrace()
                    }
                    Toast.makeText(context, text(R.string.hc_savefailed), Toast.LENGTH_LONG).show()
                    false
                }
                title = text(titleRes)
                summary = text(if (readable) summaryRes else R.string.hc_read_failed)
            })
        }
        fun add(preference: AndroidPreference) = apply { group.addPreference(preference) }
    }
    internal class IconStyle(context: Context) {
        val resources = context.resources
        private val host = context.packageName
        private val sizeRes = resources.getIdentifier("header_icon_size", "dimen", host)
        val size = if (sizeRes != 0) resources.getDimensionPixelSize(sizeRes)
            else (resources.displayMetrics.density * 28f).toInt()
        val layout = resources.getIdentifier("miuix_preference_settings_main_layout", "layout", host)
    }
    object Pages {
        private const val OTHER = "hc.other"
        const val EXTENDED = "hc.power_menu_settings"
        const val ADS = "hc.ad_service_settings"
        private class Route(val target: String, val content: (Page) -> Unit, val titleRes: Int = 0) {
            val shortcut get() = target == EXTENDED || target == ADS
            fun title(page: Page, app: ApplicationInfo? = null): String = when {
                titleRes != 0 -> page.text(titleRes)
                app != null -> app.loadLabel(page.context.packageManager).toString()
                else -> Strings.appLabel(page.context, target)
            }
        }
        private val routes = arrayOf(
            Route(FRAMEWORK, AppPages::core, R.string.hc_framework),
            Route(SYSTEM_UI, AppPages::systemUi),
            Route(POWER, AppPages::power),
            Route(HTML, AppPages::html),
            Route(DOWNLOADS, AppPages::downloads),
            Route(XMS, AppPages::xms),
            Route(WECHAT, AppPages::wechat),
            Route(OTHER, AppPages::other, R.string.hc_other),
            Route(EXTENDED, ShortcutPages::extended, R.string.hc_extended),
            Route(ADS, ShortcutPages::ads, R.string.hc_ads),
        )
        internal fun create(manager: PreferenceManager, target: String?, homeTitle: String?): PreferenceScreen {
            val screen = manager.createPreferenceScreen(manager.context)
            val page = Page(screen)
            val route = routes.firstOrNull { it.target == target }
            screen.title = route?.title(page) ?: if (target != null) Strings.appLabel(page.context, target)
                else homeTitle ?: Strings.appLabel(page.context, Strings.MODULE)
            when {
                target == null -> {
                    page.group(null)
                    for (entry in routes) if (!entry.shortcut) link(page, entry)
                }
                route != null -> route.content(page)
                else -> page.add(Preference(page.context).apply { title = page.text(R.string.hc_unavailable) })
            }
            return screen
        }
        @JvmStatic
        fun link(page: Page, target: String) {
            routes.firstOrNull { it.target == target }?.let { link(page, it) }
        }
        private fun link(page: Page, route: Route) {
            val context = page.context
            val pm = context.packageManager
            val app = try {
                if (route.shortcut || route.target == OTHER) null else pm.getApplicationInfo(route.target, 0)
            } catch (_: PackageManager.NameNotFoundException) {
                return
            }
            val row = if (route.shortcut) Preference(context).apply { setIconSpaceReserved(false) }
                else IconPreference(context, true).also { setIcon(it, page, pm, app) }
            val label = route.title(page, app)
            val args = Bundle().apply {
                putString("target", route.target)
                putString(TITLE_KEY, label)
            }
            val host = context.packageName
            row.title = label
            row.intent = Intent().setClassName(host, "$host.SubSettings")
                .putExtra(":settings:show_fragment", SettingsHook.SCREEN)
                .putExtra(":settings:show_fragment_args", args)
                .putExtra(":settings:show_fragment_title", label)
                .putExtra(":settings:source_metrics", 0)
            page.add(row)
        }
        private fun setIcon(row: IconPreference, page: Page, pm: PackageManager, app: ApplicationInfo?) {
            try {
                val style = page.iconStyle
                val size = style.size
                val icon = if (app != null) pm.getApplicationIcon(app) else {
                    val host = page.context.packageName
                    val resources = style.resources
                    var id = resources.getIdentifier("ic_other_advanced_settings", "drawable", host)
                    if (id == 0) id = resources.getIdentifier("ic_more_applications_settings", "drawable", host)
                    if (id == 0) pm.defaultActivityIcon else page.context.getDrawable(id)
                } ?: return
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                bitmap.density = style.resources.displayMetrics.densityDpi
                icon.setBounds(0, 0, size, size)
                icon.draw(Canvas(bitmap))
                row.icon = BitmapDrawable(style.resources, bitmap)
                if (style.layout != 0) row.layoutResource = style.layout
            } catch (error: Throwable) {
                error.printStackTrace()
            }
        }
    }
    private object Restart {
        private val provider = Uri.parse("content://com.cinexaft.hypercontrol.restart")
        private val busy = AtomicBoolean(false)
        private val queue = Executors.newSingleThreadExecutor { Thread(it, "HyperControlRestart") }
        fun request(activity: Activity?, target: String) {
            if (activity == null || activity.isFinishing || activity.isDestroyed || !canRestart(target)) return
            val context = activity.applicationContext
            if (!busy.compareAndSet(false, true)) {
                notify(context, Strings.get(context, R.string.hc_busy))
                return
            }
            if (target != FRAMEWORK) { enqueue(context, target); return }
            var submitted = false
            try {
                val resources = Strings.resources(activity)
                AlertDialog.Builder(activity)
                    .setTitle(Strings.get(resources, R.string.hc_rebootquestion))
                    .setMessage(Strings.get(resources, R.string.hc_confirmsystem))
                    .setPositiveButton(Strings.get(resources, R.string.hc_reboot)) { _, _ ->
                        submitted = true
                        enqueue(context, target)
                    }
                    .setNegativeButton(Strings.get(resources, R.string.hc_cancel), null)
                    .setOnDismissListener { if (!submitted) busy.set(false) }
                    .show()
            } catch (error: RuntimeException) {
                busy.set(false)
                Log.e("HyperControl", "Cannot show reboot confirmation", error)
                notify(context, Strings.get(context, R.string.hc_unavailable))
            }
        }
        private fun enqueue(context: Context, target: String) {
            try {
                queue.execute {
                    try {
                        val result = context.contentResolver.call(provider, RestartProvider.METHOD, target, null)
                        notify(context, result?.getString("message") ?: Strings.get(context, R.string.hc_call_failed))
                    } catch (error: RuntimeException) {
                        Log.e("HyperControl", "Restart provider call failed", error)
                        notify(context, Strings.get(context, R.string.hc_call_failed))
                    } finally { busy.set(false) }
                }
            } catch (error: RuntimeException) {
                busy.set(false)
                Log.e("HyperControl", "Cannot submit restart request", error)
                notify(context, Strings.get(context, R.string.hc_call_failed))
            }
        }
        private fun notify(context: Context, text: String) {
            Handler(Looper.getMainLooper()).post { Toast.makeText(context, text, Toast.LENGTH_LONG).show() }
        }
    }
    companion object {
        const val TITLE_KEY = "title"
    }
}
