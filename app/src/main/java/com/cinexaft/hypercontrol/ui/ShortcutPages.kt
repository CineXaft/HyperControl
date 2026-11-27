package com.cinexaft.hypercontrol.ui

import android.content.Intent; import com.android.settingslib.miuisettings.preference.Preference; import com.cinexaft.hypercontrol.R; import com.cinexaft.hypercontrol.base.NativeScreen.Page; import com.cinexaft.hypercontrol.base.PageBridge.Strings

/** Shortcut destinations, in display order. Titles use module resources or installed app labels. */
object ShortcutPages {
    private fun row(page: Page, title: Int): Preference {
        val row = Preference(page.context)
        row.title = page.text(title)
        row.setIconSpaceReserved(false)
        page.add(row)
        return row
    }
    private fun activity(page: Page, title: Int, pkg: String, cls: String) {
        row(page, title).intent = Intent().setClassName(pkg, cls)
    }
    @JvmStatic fun extended(page: Page) {
        page.group(R.string.hc_extended)
        activity(page, R.string.shortcut_notification_history, "com.android.settings", "com.android.settings.notification.history.NotificationHistoryActivity")
        activity(page, R.string.shortcut_extra_dim, "com.android.settings", "com.android.settings.Settings\$ReduceBrightColorsSettingsActivity")
        activity(page, R.string.shortcut_hidden_apps, "com.miui.securitycenter", "com.miui.privacyapps.ui.PrivacyAppsManageActivity")
        row(page, R.string.shortcut_all_apps).fragment = "com.android.settings.applications.manageapplications.ManageApplications"
        row(page, R.string.shortcut_privacy_options).intent = Intent("android.settings.PRIVACY_SETTINGS")
        activity(page, R.string.shortcut_high_refresh_rate_apps, "com.xiaomi.misettings", "com.xiaomi.misettings.display.RefreshRate.HighRefreshOptionsActivity")
        activity(page, R.string.shortcut_device_diagnostics, "com.android.devicediagnostics", "com.android.devicediagnostics.MainActivity")
        activity(page, R.string.shortcut_speaker_cleaning, "com.miui.misound", "com.miui.misound.cleanspeaker.CleanSpeakerActivity")
        page.group(R.string.shortcut_stock_android_features)
        activity(page, R.string.shortcut_memory_usage, "com.android.settings", "com.android.settings.Settings\$AppMemoryUsageActivity")
        row(page, R.string.shortcut_night_light).fragment = "com.android.settings.display.NightDisplaySettings"
        activity(page, R.string.shortcut_battery_optimization, "com.android.settings", "com.android.settings.Settings\$AppBatteryUsageActivity")
        activity(page, R.string.shortcut_file_manager, "com.google.android.documentsui", "com.android.documentsui.files.FilesActivity")
        activity(page, R.string.shortcut_display_size, "com.android.settings", "com.android.settings.Settings\$TextReadingSettingsActivity")
        activity(page, R.string.shortcut_location_services, "com.android.settings", "com.android.settings.Settings\$ScanningSettingsActivity")
        activity(page, R.string.shortcut_usage_statistics, "com.android.settings", "com.android.settings.UsageStatsActivity")
        page.group(R.string.shortcut_easter_eggs)
        activity(page, R.string.shortcut_android_easter_egg, "android", "com.android.internal.app.PlatLogoActivity")
        activity(page, R.string.shortcut_paint, "com.android.egg", "com.android.egg.paint.PaintActivity")
        activity(page, R.string.shortcut_squares, "com.android.egg", "com.android.egg.quares.QuaresActivity")
        activity(page, R.string.shortcut_dessert_case, "com.android.systemui", "com.android.systemui.DessertCase")
        activity(page, R.string.shortcut_spaceship, "com.android.egg", "com.android.egg.landroid.MainActivity")
    }
    @JvmStatic fun ads(page: Page) {
        page.group(Strings.appLabel(page.context, "com.miui.securitycenter"))
        activity(page, R.string.shortcut_recommendation_ads, "com.miui.securitycenter", "com.miui.securityscan.ui.settings.SettingsPrivacyActivity")
        activity(page, R.string.shortcut_app_ads, "com.miui.securitycenter", "com.miui.appmanager.AppManagerSettings")
        page.group(Strings.appLabel(page.context, "com.xiaomi.market"))
        activity(page, R.string.shortcut_rewards_ads, "com.xiaomi.market", "com.xiaomi.market.business_ui.settings.function.FunctionSettingsFragmentActivity")
        activity(page, R.string.shortcut_service_ads, "com.xiaomi.market", "com.xiaomi.market.business_ui.settings.recommend.RecommendSettingsFragmentActivity")
        activity(page, R.string.shortcut_notification_ads, "com.xiaomi.market", "com.xiaomi.market.ui.NotificationSettingsActivity")
        page.group(R.string.shortcut_system_and_app_ads)
        activity(page, R.string.shortcut_system_ads, "com.xiaomi.account", "com.xiaomi.account.settings.SystemAdActivity")
        activity(page, R.string.shortcut_cleaner_ads, "com.miui.cleanmaster", "com.miui.optimizecenter.settings.SettingsActivity")
        activity(page, R.string.shortcut_download_ads, "com.android.providers.downloads.ui", "com.android.providers.downloads.ui.activity.InfoFlowSettingActivity")
        activity(page, R.string.shortcut_calendar_ads, "com.android.calendar", "com.android.calendar.settings.UserExperienceActionBarActivity")
        activity(page, R.string.shortcut_themes_ads, "com.android.thememanager", "com.android.thememanager.activity.PrivacySettingsActivity")
        activity(page, R.string.shortcut_wallet_ads, "com.mipay.wallet", "com.mipay.wallet.app.settings.MiuiStyleSettingActivity")
        activity(page, R.string.shortcut_community_ads, "com.xiaomi.vipaccount", "com.xiaomi.vipaccount.ui.setting.HighSettingsActivity")
        activity(page, R.string.shortcut_business_hall_ads, "com.android.contacts", "com.mobile.businesshall.ui.about.BhAboutActivity")
        activity(page, R.string.shortcut_app_vault_ads, "com.miui.personalassistant", "com.miui.personalassistant.settings.PASettingActivity")
        activity(page, R.string.shortcut_package_installer_ads, "com.miui.packageinstaller", "com.android.packageinstaller.SettingsActivity")
        activity(page, R.string.shortcut_browser_ads, "com.android.browser", "com.android.browser.preferences.activity.CustomizationPreferenceActivity")
        activity(page, R.string.shortcut_mi_music_ads, "com.miui.player", "com.miui.player.phone.ui.MusicSettings")
        activity(page, R.string.shortcut_voice_assistant_ads, "com.miui.voiceassist", "com.xiaomi.voiceassistant.settings.PrivacySettingsActivity")
        activity(page, R.string.shortcut_sogou_keyboard_ads, "com.sohu.inputmethod.sogou.xiaomi", "com.sohu.inputmethod.settings.activity.AdvertisementSettings")
        activity(page, R.string.shortcut_mi_video_service, "com.miui.video", "com.miui.video.feature.mine.setting.DevelopSettingActivity")
        activity(page, R.string.shortcut_youpin_ads, "com.xiaomi.youpin", "com.xiaomi.profile.model.setting.SettingActivity")
        activity(page, R.string.shortcut_hyperos_engine_ads, "com.xiaomi.aicr", "com.xiaomi.aicr.PrivacyManagerActivity")
        activity(page, R.string.shortcut_mi_video_ads, "com.miui.video", "com.miui.video.feature.mine.setting.SettingActivity")
        activity(page, R.string.shortcut_global_search_ads, "com.android.quicksearchbox", "com.android.quicksearchbox.preferences.SearchSettingsPreferenceActivity")
    }
}
