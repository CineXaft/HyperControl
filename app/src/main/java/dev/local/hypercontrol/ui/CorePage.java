package dev.local.hypercontrol.ui;

import android.content.Context;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.miuisettings.preference.PreferenceCategory;
import dev.local.hypercontrol.core.Keys;

/* JADX INFO: loaded from: classes.dex */
public final class CorePage {
    private static void add(Context context, PreferenceGroup preferenceGroup, int i) {
        String strKey = Keys.key(i);
        String string = Integer.toString(i);
        Catalog.option(context, preferenceGroup, strKey, "hc_cp".concat(string), "hc_cpd".concat(string));
    }

    public static void fill(Context context, PreferenceScreen preferenceScreen) {
        PreferenceCategory preferenceCategory = new PreferenceCategory(context);
        preferenceCategory.setTitle(Strings.get(context, "hc_framework"));
        preferenceScreen.addPreference(preferenceCategory);
        Catalog.option(context, preferenceCategory, "system_framework_flag_secure", "hc_secure", "hc_secure_desc");
        Catalog.option(context, preferenceCategory, "system_framework_passkey", "hc_passkey", "hc_passkey_desc");
        Catalog.option(context, preferenceCategory, "system_framework_safe_volume", "hc_safe_volume", "hc_safe_volume_desc");
        PreferenceCategory preferenceCategory2 = new PreferenceCategory(context);
        preferenceCategory2.setTitle(Strings.get(context, "hc_n_title"));
        preferenceScreen.addPreference(preferenceCategory2);
        for (int i = 0; i < 10; i++) {
            add(context, preferenceCategory2, i);
        }
    }
}
