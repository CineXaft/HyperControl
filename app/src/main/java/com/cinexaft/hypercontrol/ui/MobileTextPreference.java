package com.cinexaft.hypercontrol.ui;

import android.content.Context;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;
import androidx.preference.Preference;
import com.android.settingslib.miuisettings.preference.EditTextPreference;

/* JADX INFO: loaded from: classes.dex */
public final class MobileTextPreference extends EditTextPreference implements Preference.OnPreferenceChangeListener, androidx.preference.EditTextPreference.OnBindEditTextListener {
    public MobileTextPreference(Context context) {
        super(context);
        setKey("mobile_type_text");
        setPersistent(false);
        String str = Strings.get(context, "hc_mobile_text");
        setTitle(str);
        setDialogTitle(str);
        setPositiveButtonText(Strings.get(context, "hc_save"));
        setNegativeButtonText(Strings.get(context, "hc_cancel"));
        String str2 = "";
        try {
            String string = Settings.Global.getString(context.getContentResolver(), "hypercontrol_mobile_type_text");
            if (string != null) {
                str2 = string;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        setText(str2);
        summary(str2);
        setOnPreferenceChangeListener(this);
        setOnBindEditTextListener(this);
    }

    private void summary(String str) {
        if (str.isEmpty()) {
            str = Strings.get(getContext(), "hc_empty_text");
        }
        setSummary(str);
    }

    public void onBindEditText(EditText editText) {
        editText.setInputType(1);
        editText.setSingleLine(true);
        editText.setHint(Strings.get(getContext(), "hc_empty_text"));
    }

    public boolean onPreferenceChange(Preference preference, Object obj) {
        String str;
        try {
            String str2 = (String) obj;
            if (str2.contains("\n") || str2.contains("\r")) {
                str = "hc_singleline";
            } else {
                if (Settings.Global.putString(getContext().getContentResolver(), "hypercontrol_mobile_type_text", str2)) {
                    summary(str2);
                    return true;
                }
                str = "hc_textfailed";
            }
        } catch (Throwable th) {
            th.printStackTrace();
            str = "hc_textfailed";
        }
        Context context = getContext();
        Toast.makeText(context, Strings.get(context, str), 1).show();
        return false;
    }
}
