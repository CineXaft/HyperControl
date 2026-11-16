package dev.local.hypercontrol.ui;

import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;
import androidx.preference.Preference;
import com.android.settingslib.miuisettings.preference.SwitchPreference;
import dev.local.hypercontrol.Platform;
import dev.local.hypercontrol.State;

/* JADX INFO: loaded from: classes.dex */
public final class FeatureSwitch extends SwitchPreference implements Preference.OnPreferenceChangeListener {
    public FeatureSwitch(Context context, String str) {
        super(context);
        setKey(str);
        setPersistent(false);
        try {
            setChecked(Settings.Global.getInt(context.getContentResolver(), State.setting(str), 0) == 1);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        if ("keep_clock_expanded".equals(str) && Platform.clockUnsupported()) {
            setEnabled(false);
        }
        setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object obj) {
        try {
            if (Settings.Global.putInt(getContext().getContentResolver(), State.setting(getKey()), ((Boolean) obj).booleanValue() ? 1 : 0)) {
                return true;
            }
            throw new IllegalStateException("SettingsProvider rejected save");
        } catch (Throwable th) {
            th.printStackTrace();
            Context context = getContext();
            Toast.makeText(context, Strings.get(context, "hc_savefailed"), 1).show();
            return false;
        }
    }
}
