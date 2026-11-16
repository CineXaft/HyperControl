package com.android.settingslib.miuisettings.preference;

import android.content.Context;

/** Compile-only MIUI SettingsLib shim. Device runtime uses the original platform implementation. */
public class IconPreference extends Preference {
    public IconPreference(Context context, boolean showIcon) { super(context); }
}
