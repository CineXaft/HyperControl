package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.util.AttributeSet;

/** Compile-only MIUI SettingsLib shim. Device runtime uses the original platform implementation. */
public class SwitchPreference extends androidx.preference.SwitchPreferenceCompat {
    public SwitchPreference(Context context) { super(context); }
    public SwitchPreference(Context context, AttributeSet attrs) { super(context, attrs); }
}
