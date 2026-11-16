package dev.local.hypercontrol.flasher;

import android.R;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.android.settingslib.miuisettings.preference.PreferenceCategory;
import dev.local.hypercontrol.ui.NativeScreen;
import dev.local.hypercontrol.ui.Strings;
import miuix.appcompat.app.AlertDialog;

/* JADX INFO: loaded from: classes.dex */
public final class Page implements Preference.OnPreferenceClickListener, DialogInterface.OnClickListener, Runnable {
    private static boolean flashDismissed;
    private static boolean rebootDismissed;
    private boolean active;
    private boolean busy;
    private int confirmKind;
    private final NativeScreen fragment;
    private String info;
    private String lastToken;
    private Preference logButton;
    private AlertDialog logDialog;
    private TextView logText;
    private Preference output;
    private Preference pick;
    private String raw;
    private Preference reboot;
    private boolean selectionPending;
    private Preference status;
    private String text;
    private String token;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int lastPhase = -1;

    public Page(NativeScreen nativeScreen) {
        this.fragment = nativeScreen;
    }

    private String clip(String str) {
        int length;
        return (str == null || (length = str.length()) <= 1500) ? str : str.substring(length - 1500);
    }

    private void confirm(int i) {
        this.confirmKind = i;
        AlertDialog.Builder builder = new AlertDialog.Builder(context());
        String str = "hc_kernel_flash";
        String str2 = "hc_kernel_confirm";
        if (i != 0) {
            str = "hc_reboot";
            str2 = "hc_confirmsystem";
        }
        builder.setTitle(t(str));
        String strT = t(str2);
        if (i == 0) {
            strT = strT + "\n\n" + this.info;
        }
        builder.setMessage(strT);
        builder.setPositiveButton(t(i != 0 ? "hc_reboot" : "hc_kernel_flash"), this);
        builder.setNegativeButton(t("hc_cancel"), this);
        builder.show();
    }

    private Context context() {
        return this.fragment.getPreferenceManager().getContext();
    }

    private void fail(Throwable th) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context());
        builder.setTitle(t("hc_kernel_error"));
        builder.setMessage(th.toString());
        builder.setPositiveButton(R.string.ok, (DialogInterface.OnClickListener) null);
        builder.show();
    }

    private void failText(String str) {
        if (str != null) {
            int length = str.length();
            if (length > 600) {
                str = str.substring(length - 600);
            }
            fail(new IllegalStateException(str));
        }
    }

    private boolean isRebootDismissed() {
        return context().getSharedPreferences("HyperControlKernelFlasher", 0).getBoolean("reboot_dismissed", false);
    }

    private Bundle request(String str, String str2) {
        ContentResolver contentResolver = context().getContentResolver();
        Uri uri = Uri.parse("content://com.cinexaft.hypercontrol.flasher");
        int i = 0;
        while (true) {
            Bundle bundleCall = contentResolver.call(uri, str, str2, (Bundle) null);
            if (bundleCall != null) {
                String string = bundleCall.getString("error");
                if (string != null) {
                    throw new IllegalStateException(string);
                }
                return bundleCall;
            }
            i++;
            if (i >= 3) {
                throw new IllegalStateException("Module service unavailable");
            }
            SystemClock.sleep(120L);
        }
    }

    private Preference row(PreferenceGroup preferenceGroup, String str, boolean z) {
        com.android.settingslib.miuisettings.preference.Preference preference = new com.android.settingslib.miuisettings.preference.Preference(context());
        preference.setTitle(t(str));
        preference.setIconSpaceReserved(false);
        preference.setSelectable(z);
        if (z) {
            preference.setOnPreferenceClickListener(this);
        }
        preferenceGroup.addPreference(preference);
        return preference;
    }

    private void saveRebootDismissed(boolean z) {
        context().getSharedPreferences("HyperControlKernelFlasher", 0).edit().putBoolean("reboot_dismissed", z).commit();
    }

    private void showLog() {
        String str = this.text;
        if (str == null || str.isEmpty()) {
            str = "暂无日志";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context());
        builder.setTitle(t("hc_kernel_log"));
        builder.setMessage(str);
        builder.setNegativeButton("复制", new LogCopy(this));
        builder.setPositiveButton("关闭", (DialogInterface.OnClickListener) null);
        this.logDialog = builder.show();
    }

    private String state(int i) {
        String str = i == 1 ? "hc_kernel_preparing" : "hc_kernel_idle";
        if (i == 2) {
            str = "hc_kernel_ready";
        }
        if (i == 3) {
            str = "hc_kernel_running";
        }
        if (i == 4) {
            str = "hc_kernel_done";
        }
        if (i == 5) {
            str = "hc_kernel_error";
        }
        if (i == 6) {
            str = "hc_kernel_running";
        }
        return t(str);
    }

    private String t(String str) {
        return Strings.get(context(), str);
    }

    public void apply(Bundle bundle) {
        if (bundle != null) {
            String string = bundle.getString("error");
            if (string != null) {
                Log.w("HyperControl.Flasher", string);
                return;
            }
            try {
                int i = bundle.getInt("phase");
                if (isRebootDismissed()) {
                    rebootDismissed = true;
                }
                if (bundle.getBoolean("rebootDismissed")) {
                    rebootDismissed = true;
                }
                String string2 = bundle.getString("token");
                String string3 = bundle.getString("info");
                String string4 = bundle.getString("log");
                String string5 = bundle.getString("raw");
                this.token = string2;
                this.info = string3;
                this.text = string4;
                this.raw = string5;
                AlertDialog alertDialog = this.logDialog;
                if (alertDialog != null) {
                    alertDialog.setMessage(string4);
                }
                this.pick.setEnabled(i != 3);
                if (i != this.lastPhase) {
                    this.lastPhase = i;
                    if (i == 3) {
                        showLog();
                    }
                    if (i == 2) {
                        if (this.selectionPending) {
                            this.selectionPending = false;
                            if (flashDismissed) {
                                return;
                            }
                            confirm(0);
                            return;
                        }
                        return;
                    }
                    if (i == 4) {
                        if (rebootDismissed) {
                            return;
                        }
                        confirm(1);
                    } else if (i == 5) {
                        if (string5 == null || string5.isEmpty()) {
                            string5 = string4;
                        }
                        AlertDialog alertDialog2 = this.logDialog;
                        if (alertDialog2 == null || !alertDialog2.isShowing()) {
                            failText(string5);
                        }
                    }
                }
            } catch (Throwable th) {
                Log.w("HyperControl.Flasher", "Status UI update failed", th);
            }
        }
    }

    public boolean click(Preference preference) {
        try {
            if (preference != this.pick) {
                if (preference != this.logButton) {
                    return true;
                }
                showLog();
                return true;
            }
            Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT");
            intent.setType("*/*");
            intent.addCategory("android.intent.category.OPENABLE");
            intent.addFlags(1);
            this.fragment.startActivityForResult(intent, 18507);
            return true;
        } catch (Throwable th) {
            fail(th);
            return true;
        }
    }

    public void copy() {
        ClipboardManager clipboardManager = (ClipboardManager) context().getSystemService("clipboard");
        String str = this.text;
        if (str != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText("Kernel flasher", str));
        }
    }

    public void done() {
        this.busy = false;
        if (this.active) {
            this.handler.postDelayed(this, 750L);
        }
    }

    public void failDirect(Throwable th) {
        fail(th);
    }

    public void failTo(Throwable th) {
        this.handler.post(new Report(this, th));
    }

    public Bundle fetch() {
        try {
            return request("status", null);
        } catch (Throwable th) {
            Bundle bundle = new Bundle();
            bundle.putString("error", th.toString());
            return bundle;
        }
    }

    public void fill(PreferenceScreen preferenceScreen) {
        PreferenceCategory preferenceCategory = new PreferenceCategory(context());
        preferenceScreen.addPreference(preferenceCategory);
        this.pick = row(preferenceCategory, "hc_kernel_select", true);
        this.logButton = row(preferenceCategory, "hc_kernel_log", true);
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        String str;
        String str2;
        if (i == -2) {
            if (this.confirmKind == 0) {
                flashDismissed = true;
                submit("discard", null);
                return;
            } else {
                rebootDismissed = true;
                saveRebootDismissed(true);
                submit("dismiss_reboot", null);
                return;
            }
        }
        if (i == -1) {
            if (this.confirmKind == 0) {
                str = "start";
                str2 = this.token;
            } else {
                str = "reboot";
                str2 = null;
            }
            new Thread(new Push(this, str, str2), "HyperControlPush").start();
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        return click(preference);
    }

    public void postApply(Bundle bundle) {
        if (bundle != null) {
            this.handler.post(new Poll(this, bundle));
        }
    }

    public void result(int i, int i2, Intent intent) {
        Uri data;
        if (i != 18507 || i2 != -1 || intent == null || (data = intent.getData()) == null) {
            return;
        }
        rebootDismissed = false;
        flashDismissed = false;
        try {
            Context context = context();
            saveRebootDismissed(false);
            request("discard", null);
            this.selectionPending = true;
            this.lastPhase = -1;
            start();
            new Thread(new Copy(this, context, data), "HyperControlCopy").start();
        } catch (Throwable th) {
            fail(th);
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        if (!this.active || this.busy) {
            return;
        }
        this.busy = true;
        new Thread(new Poll(this), "HyperControlPoll").start();
    }

    public void start() {
        this.active = true;
        Handler handler = this.handler;
        handler.removeCallbacks(this);
        handler.post(this);
    }

    public void stop() {
        this.active = false;
        this.handler.removeCallbacks(this);
    }

    public void submit(String str, String str2) {
        try {
            postApply(request(str, str2));
        } catch (Throwable th) {
            failTo(th);
        }
    }

    public void ui(Runnable runnable) {
        this.handler.post(runnable);
    }
}
