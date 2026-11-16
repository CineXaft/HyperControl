package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.util.AttributeSet;

/** Compile-only MIUI SettingsLib shim. Device runtime uses the original platform implementation. */
public class PreferenceCategory extends androidx.preference.PreferenceCategory {
    public PreferenceCategory(Context context) { super(context); }
    public PreferenceCategory(Context context, AttributeSet attrs) { super(context, attrs); }
}
