package com.android.settings;

import android.view.Menu;
import android.view.MenuInflater;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/** Compile-only shim for a Settings private API; the platform provides the runtime class. */
public abstract class SettingsPreferenceFragment extends PreferenceFragmentCompat {
    public int getMetricsCategory() { return 0; }
    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) { super.onCreateOptionsMenu(menu, inflater); }
    public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) { return false; }
}
