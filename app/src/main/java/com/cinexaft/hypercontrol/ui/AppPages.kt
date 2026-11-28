package com.cinexaft.hypercontrol.ui

import android.content.Intent; import com.android.settingslib.miuisettings.preference.Preference; import com.cinexaft.hypercontrol.R; import com.cinexaft.hypercontrol.base.Entry.Keys; import com.cinexaft.hypercontrol.base.Entry.Targets.WECHAT; import com.cinexaft.hypercontrol.base.Entry.Targets.XMS
import com.cinexaft.hypercontrol.base.NativeScreen.Page; import com.cinexaft.hypercontrol.base.NativeScreen.Pages; import com.cinexaft.hypercontrol.base.PageBridge.Strings

/** Preference content builders, kept together because each is a thin route target. */
object AppPages {
    @JvmStatic fun core(page: Page) {
        page.group(R.string.hc_framework)
            .option("system_framework_flag_secure", R.string.hc_secure, R.string.hc_secure_desc)
            .option("system_framework_passkey", R.string.hc_passkey, R.string.hc_passkey_desc)
            .option("system_framework_safe_volume", R.string.hc_safe_volume, R.string.hc_safe_volume_desc)
            .group(R.string.hc_n_title)
        val titles = intArrayOf(R.string.hc_cp0, R.string.hc_cp1, R.string.hc_cp2, R.string.hc_cp3,
            R.string.hc_cp4, R.string.hc_cp5, R.string.hc_cp6, R.string.hc_cp7, R.string.hc_cp8, R.string.hc_cp9)
        val summaries = intArrayOf(R.string.hc_cpd0, R.string.hc_cpd1, R.string.hc_cpd2, R.string.hc_cpd3,
            R.string.hc_cpd4, R.string.hc_cpd5, R.string.hc_cpd6, R.string.hc_cpd7, R.string.hc_cpd8, R.string.hc_cpd9)
        for (i in titles.indices) page.option(Keys.key(i), titles[i], summaries[i])
    }
    @JvmStatic fun systemUi(page: Page) {
        page.group(R.string.hc_ui1)
            .feature(0, R.string.hc_ui6, R.string.hc_ui17)
            .feature(7, R.string.hc_ui7, R.string.hc_ui18)
            .feature(8, R.string.hc_ui8, R.string.hc_ui19)
            .feature(9, R.string.hc_ui9, R.string.hc_ui20)
            .feature(10, R.string.hc_ui10, R.string.hc_ui21)
            .feature(12, R.string.hc_ui11, R.string.hc_ui22)
            .add(MobileTextPreference(page.context))
        page.group(R.string.hc_lockscreen)
            .feature(22, R.string.hc_ui12, R.string.hc_ui23)
    }
    @JvmStatic fun wechat(page: Page) {
        page.group(Strings.appLabel(page.context, WECHAT))
            .feature(17, R.string.hc_wechat_pad, R.string.hc_wechat_pad_desc)
            .feature(18, R.string.hc_wechat_select_original, R.string.hc_wechat_select_original_desc)
            .feature(20, R.string.hc_wechat_no_typing, R.string.hc_wechat_no_typing_desc)
            .feature(21, R.string.hc_wechat_hide_sns_ads, R.string.hc_wechat_hide_sns_ads_desc)
    }
    @JvmStatic fun other(page: Page) {
        page.group(null)
        Pages.link(page, Pages.EXTENDED)
        Pages.link(page, Pages.ADS)
        val google = "com.google.android.gms"
        page.add(Preference(page.context).apply {
            title = Strings.appLabel(page.context, google)
            setIconSpaceReserved(false)
            intent = Intent(Intent.ACTION_VIEW).setClassName(google,
                "com.google.android.gms.app.settings.GoogleSettingsLink")
        })
    }
    @JvmStatic fun power(page: Page) {
        page.group(R.string.hc_ui3)
            .feature(3, R.string.hc_ui13, R.string.hc_ui24)
            .feature(4, R.string.hc_ui14, R.string.hc_ui25)
    }
    @JvmStatic fun html(page: Page) {
        page.group(R.string.hc_ui4).feature(5, R.string.hc_ui15, R.string.hc_ui26)
    }
    @JvmStatic fun downloads(page: Page) {
        page.group(R.string.hc_ui5).feature(6, R.string.hc_ui16, R.string.hc_ui27)
    }
    @JvmStatic fun xms(page: Page) {
        page.group(Strings.appLabel(page.context, XMS))
            .feature(13, R.string.hc_xms_auth, R.string.hc_xms_auth_desc)
    }
}
