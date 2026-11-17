package dev.local.hypercontrol.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.miuisettings.preference.Preference;
import dev.local.hypercontrol.RestartTarget;
import dev.local.hypercontrol.flasher.Page;

/* JADX INFO: loaded from: classes.dex */
public final class NativeScreen extends SettingsPreferenceFragment {
    private Page kernel;
    private String target;
    private String title;

    private String safeLabel(Context context, String packageName) {
        try {
            return RestartTarget.label(context, packageName);
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private void addRestart(Menu menu) {
        int identifier;
        String str = this.target;
        if (str == null || str.startsWith("hc.") || menu.findItem(1212371457) != null) {
            return;
        }
        String strConcat = Strings.get(getPreferenceManager().getContext(), "hc_restart").concat(" ").concat(this.title);
        if ("android".equals(this.target)) {
            strConcat = Strings.get(getPreferenceManager().getContext(), "hc_reboot");
        }
        MenuItem menuItemAdd = menu.add(0, 1212371457, 0, strConcat);
        menuItemAdd.setOnMenuItemClickListener(new RestartMenu(getActivity(), this.target));
        Context context = getPreferenceManager().getContext();
        Resources resources = context.getResources();
        int identifier2 = resources.getIdentifier("actionBarRefreshIcon", "attr", "com.android.settings");
        TypedValue typedValue = new TypedValue();
        if (!context.getTheme().resolveAttribute(identifier2, typedValue, true) || (identifier = typedValue.resourceId) == 0) {
            identifier = resources.getIdentifier("action_button_refresh", "drawable", "com.android.settings");
        }
        if (identifier != 0) {
            menuItemAdd.setIcon(identifier);
        }
        menuItemAdd.setShowAsAction(2);
    }

    public int getMetricsCategory() {
        return 0;
    }

    public void onActivityResult(int i, int i2, Intent intent) {
        super.onActivityResult(i, i2, intent);
        Page page = this.kernel;
        if (page != null) {
            page.result(i, i2, intent);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        addRestart(menu);
    }

    public void onCreatePreferences(Bundle bundle, String str) {
        this.title = Strings.get(getPreferenceManager().getContext(), "hc_modules");
        Bundle arguments = getArguments();
        if (arguments != null) {
            String string = arguments.getString("title");
            if (string != null) {
                this.title = string;
            }
            String string2 = arguments.getString("target");
            if (string2 != null) {
                this.target = string2;
                this.title = safeLabel(getPreferenceManager().getContext(), string2);
            } else if (arguments.getBoolean("hc_systemui")) {
                string2 = "com.android.systemui";
                this.target = string2;
                this.title = safeLabel(getPreferenceManager().getContext(), string2);
            }
        }
        PreferenceManager preferenceManager = getPreferenceManager();
        Context context = preferenceManager.getContext();
        PreferenceScreen preferenceScreenCreatePreferenceScreen = preferenceManager.createPreferenceScreen(context);
        preferenceScreenCreatePreferenceScreen.setTitle(this.title);
        setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
        String str3 = this.target;
        if (str3 != null && ("hc.power_menu_settings".equals(str3) || "hc.ad_service_settings".equals(str3))) {
            String str2 = "hc.power_menu_settings".equals(str3) ? "power_menu_settings" : "ad_service_settings";
            try {
                preferenceScreenCreatePreferenceScreen = Extras.load(context, getPreferenceManager(), str2);
                setPreferenceScreen(preferenceScreenCreatePreferenceScreen);
                return;
            } catch (Throwable th) {
                th.printStackTrace();
                Preference preference = new Preference(context);
                preference.setTitle(Strings.get(context, "hc_loadfailed"));
                preferenceScreenCreatePreferenceScreen.addPreference(preference);
                return;
            }
        }
        String str4 = this.target;
        if (!"hc.kernel".equals(str4)) {
            Catalog.fill(context, preferenceScreenCreatePreferenceScreen, str4);
            return;
        }
        String str5 = Strings.get(context, "hc_kernel");
        this.title = str5;
        preferenceScreenCreatePreferenceScreen.setTitle(str5);
        Page page = new Page(this);
        this.kernel = page;
        page.fill(preferenceScreenCreatePreferenceScreen);
    }

    public void onPause() {
        Page page = this.kernel;
        if (page != null) {
            page.stop();
        }
        super.onPause();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, androidx.preference.Preference preference) {
        Page page = this.kernel;
        if (page != null && page.click(preference)) {
            return true;
        }
        Intent intent = preference.getIntent();
        if (intent == null) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            e.printStackTrace();
            Context context = getContext();
            Toast.makeText(context, Strings.get(context, "hc_unavailable"), 1).show();
            return true;
        }
    }

    public void onResume() {
        super.onResume();
        Page page = this.kernel;
        if (page != null) {
            page.start();
        }
    }

    public void onStart() {
        super.onStart();
        String str = this.title;
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.setTitle(str);
        }
        FragmentActivity activity2 = getActivity();
        if (activity2 != null) {
            activity2.setTitle(str);
        }
    }
}
