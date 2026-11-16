package com.android.settingslib.miuisettings.preference;

import android.content.Context;
import android.util.AttributeSet;

/** Compile-only MIUI SettingsLib shim. Device runtime uses the original platform implementation. */
public class EditTextPreference extends androidx.preference.EditTextPreference {
    public EditTextPreference(Context context) { super(context); }
    public EditTextPreference(Context context, AttributeSet attrs) { super(context, attrs); }
}
