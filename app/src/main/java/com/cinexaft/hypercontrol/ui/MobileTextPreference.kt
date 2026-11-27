package com.cinexaft.hypercontrol.ui

import android.content.Context; import android.provider.Settings; import android.text.InputType; import android.widget.EditText; import android.widget.Toast; import androidx.preference.Preference
import com.android.settingslib.miuisettings.preference.EditTextPreference; import com.cinexaft.hypercontrol.R; import com.cinexaft.hypercontrol.base.Entry.State.MOBILE_TEXT; import com.cinexaft.hypercontrol.base.PageBridge.Strings

class MobileTextPreference(context: Context) : EditTextPreference(context),
    Preference.OnPreferenceChangeListener,
    androidx.preference.EditTextPreference.OnBindEditTextListener {
    init {
        key = "mobile_type_text"
        isPersistent = false
        val title = Strings.get(context, R.string.hc_mobile_text)
        setTitle(title)
        setDialogTitle(title)
        setPositiveButtonText(Strings.get(context, R.string.hc_save))
        setNegativeButtonText(Strings.get(context, R.string.hc_cancel))
        var text: String? = null
        try {
            text = Settings.Global.getString(context.contentResolver, MOBILE_TEXT)
        } catch (error: Throwable) {
            error.printStackTrace()
            isEnabled = false
        }
        if (text == null) text = ""
        setText(text)
        if (isEnabled) updateSummary(text)
        else summary = Strings.get(context, R.string.hc_read_failed)
        setOnPreferenceChangeListener(this)
        setOnBindEditTextListener(this)
    }
    private fun updateSummary(textValue: String) {
        var text = textValue
        if (text.isEmpty()) text = Strings.get(context, R.string.hc_empty_text)
        if (text.codePointCount(0, text.length) > 15) {
            text = text.substring(0, text.offsetByCodePoints(0, 14)) + "…"
        }
        summary = text
    }
    override fun onBindEditText(editText: EditText) {
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.setSingleLine(true)
        editText.hint = Strings.get(context, R.string.hc_empty_text)
    }
    override fun onPreferenceChange(preference: Preference, value: Any?): Boolean {
        var errorMessage = R.string.hc_textfailed
        try {
            val text = value as String
            if (text.contains("\n") || text.contains("\r")) {
                errorMessage = R.string.hc_singleline
            } else if (Settings.Global.putString(context.contentResolver, MOBILE_TEXT, text)) {
                updateSummary(text)
                return true
            }
        } catch (error: Throwable) {
            error.printStackTrace()
        }
        Toast.makeText(context, Strings.get(context, errorMessage), Toast.LENGTH_LONG).show()
        return false
    }
}
