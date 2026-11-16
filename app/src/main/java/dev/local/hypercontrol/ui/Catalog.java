package dev.local.hypercontrol.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.miuisettings.preference.IconPreference;
import com.android.settingslib.miuisettings.preference.Preference;
import com.android.settingslib.miuisettings.preference.PreferenceCategory;
import dev.local.hypercontrol.RestartTarget;
import dev.local.hypercontrol.State;

/* JADX INFO: loaded from: classes.dex */
public final class Catalog {
    public static void feature(Context context, PreferenceGroup preferenceGroup, int i, String str, String str2) {
        option(context, preferenceGroup, State.key(i), str, str2);
    }

    public static void fill(Context context, PreferenceScreen preferenceScreen, String str) {
        PreferenceCategory preferenceCategory = new PreferenceCategory(context);
        preferenceScreen.addPreference(preferenceCategory);
        if (str == null) {
            row(context, preferenceCategory, "android", "系统框架");
            row(context, preferenceCategory, "com.android.systemui", "系统界面");
            row(context, preferenceCategory, "com.miui.powerkeeper", "电量和性能");
            row(context, preferenceCategory, "com.android.htmlviewer", "HTML 查看器");
            row(context, preferenceCategory, "com.android.providers.downloads", "下载管理");
            row(context, preferenceCategory, "com.miui.securitycenter", RestartTarget.label(context, "com.miui.securitycenter"));
            row(context, preferenceCategory, "hc.other", "其他");
            return;
        }
        if ("android".equals(str)) {
            CorePage.fill(context, preferenceScreen);
            return;
        }
        if ("hc.other".equals(str)) {
            row(context, preferenceCategory, "hc.power_menu_settings", "拓展功能");
            row(context, preferenceCategory, "hc.ad_service_settings", "广告服务");
            row(context, preferenceCategory, "hc.kernel", "hc_kernel");
            Preference preference = new Preference(context);
            preference.setTitle("Google");
            preference.setIconSpaceReserved(false);
            preference.setIntent(new Intent().setClassName("com.google.android.gms", "com.google.android.gms.app.settings.GoogleSettingsLink").setAction("android.intent.action.VIEW"));
            preferenceCategory.addPreference(preference);
            return;
        }
        if ("com.miui.securitycenter".equals(str)) {
            SecurityCenterPage.fill(context, preferenceScreen);
            return;
        }
        if ("com.android.systemui".equals(str)) {
            preferenceCategory.setTitle(Strings.get(context, "hc_ui1"));
            feature(context, preferenceCategory, 0, "hc_ui6", "hc_ui17");
            feature(context, preferenceCategory, 7, "hc_ui7", "hc_ui18");
            feature(context, preferenceCategory, 8, "hc_ui8", "hc_ui19");
            feature(context, preferenceCategory, 9, "hc_ui9", "hc_ui20");
            feature(context, preferenceCategory, 10, "hc_ui10", "hc_ui21");
            preferenceCategory.addPreference(new MobileTextPreference(context));
            PreferenceCategory preferenceCategory2 = new PreferenceCategory(context);
            preferenceCategory2.setTitle(Strings.get(context, "hc_lockscreen"));
            preferenceScreen.addPreference(preferenceCategory2);
            feature(context, preferenceCategory2, 11, "hc_clock", "hc_clock_desc");
            return;
        }
        if ("com.miui.powerkeeper".equals(str)) {
            preferenceCategory.setTitle(Strings.get(context, "hc_ui3"));
            feature(context, preferenceCategory, 3, "hc_ui13", "hc_ui24");
            feature(context, preferenceCategory, 4, "hc_ui14", "hc_ui25");
        } else if ("com.android.providers.downloads".equals(str)) {
            preferenceCategory.setTitle(Strings.get(context, "hc_ui5"));
            feature(context, preferenceCategory, 6, "hc_ui16", "hc_ui27");
        } else {
            preferenceCategory.setTitle(Strings.get(context, "hc_ui4"));
            feature(context, preferenceCategory, 5, "hc_ui15", "hc_ui26");
        }
    }

    public static void option(Context context, PreferenceGroup preferenceGroup, String str, String str2, String str3) {
        FeatureSwitch featureSwitch = new FeatureSwitch(context, str);
        featureSwitch.setTitle(Strings.t(context, str2));
        featureSwitch.setSummary(Strings.t(context, str3));
        preferenceGroup.addPreference(featureSwitch);
    }

    public static void row(Context context, PreferenceGroup preferenceGroup, String str, String str2) {
        androidx.preference.Preference preference;
        if (Compat.available(context, str)) {
            String strT = Strings.t(context, str2);
            if ("hc.kernel".equals(str) || "hc.power_menu_settings".equals(str) || "hc.ad_service_settings".equals(str)) {
                preference = new Preference(context);
                preference.setIconSpaceReserved(false);
            } else {
                preference = new IconPreference(context, true);
                Compat.icon(preference, context, str);
            }
            preference.setTitle(strT);
            Intent intent = new Intent();
            String packageName = context.getPackageName();
            intent.setClassName(packageName, packageName.concat(".SubSettings"));
            intent.putExtra(":settings:show_fragment", "dev.local.hypercontrol.ui.NativeScreen");
            Bundle bundle = new Bundle();
            bundle.putString("target", str);
            bundle.putString("title", strT);
            intent.putExtra(":settings:show_fragment_args", bundle);
            intent.putExtra(":settings:show_fragment_title", strT);
            intent.putExtra(":settings:source_metrics", 0);
            preference.setIntent(intent);
            preferenceGroup.addPreference(preference);
        }
    }
}
