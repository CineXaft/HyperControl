package com.cinexaft.hypercontrol.ui;

import android.content.Context;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.miuisettings.preference.PreferenceCategory;

/**
 * The Security Center page.
 *
 * <p>Each switch maps to a setting the Security Center process reads when it starts, so the
 * descriptions say a restart is needed.
 */
public final class SecurityCenterPage {
    public static void fill(Context context, PreferenceScreen preferenceScreen) {
        PreferenceCategory preferenceCategory = new PreferenceCategory(context);
        preferenceCategory.setTitle(Strings.get(context, "hc_sec_title"));
        preferenceScreen.addPreference(preferenceCategory);
        Catalog.option(context, preferenceCategory, "securitycenter_score_zero",
                "hc_sec_score", "hc_sec_score_desc");
        Catalog.option(context, preferenceCategory, "securitycenter_intercept_bypass",
                "hc_sec_intercept", "hc_sec_intercept_desc");
        Catalog.option(context, preferenceCategory, "securitycenter_adb_verify_bypass",
                "hc_sec_adb", "hc_sec_adb_desc");
    }
}
