package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.util.AttributeSet;

/** Compile-only MIUI SettingsLib shim. Device runtime uses the original platform implementation. */
public class Preference extends androidx.preference.Preference {
    public Preference(Context context) { super(context); }
    public Preference(Context context, AttributeSet attrs) { super(context, attrs); }
}
