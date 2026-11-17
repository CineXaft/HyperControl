package com.cinexaft.hypercontrol;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

/* JADX INFO: loaded from: classes.dex */
public final class State {
    private static volatile boolean[] flags;
    private static volatile String mobileText;

    public static void bind(Context context) {
        boolean[] zArr = new boolean[13];
        ContentResolver contentResolver = context.getContentResolver();
        for (int i = 0; i < 13; i++) {
            try {
                zArr[i] = Settings.Global.getInt(contentResolver, setting(key(i)), 0) == 1;
            } catch (Throwable th) {
                Reflect.log(th);
            }
        }
        mobileText = null;
        try {
            mobileText = Settings.Global.getString(contentResolver, "hypercontrol_mobile_type_text");
        } catch (Throwable th2) {
            Reflect.log(th2);
        }
        flags = zArr;
    }

    public static String customMobileText() {
        if (TextUtils.isEmpty(mobileText)) {
            return null;
        }
        return mobileText;
    }

    public static boolean enabled(int i) {
        boolean[] zArr = flags;
        if (zArr != null) {
            return zArr[i];
        }
        return false;
    }

    public static String key(int i) {
        switch (i) {
            case 0:
                return "hide_media_island";
            case 1:
                return "guard_disable_root";
            case 2:
                return "guard_disable_upload";
            case 3:
                return "power_dont_kill";
            case 4:
                return "power_disable_cloud";
            case 5:
                return "html_disable_cloud";
            case 6:
                return "downloads_remove_xl";
            case 7:
                return "hide_mobile_activity";
            case 8:
                return "hide_wifi_activity";
            case 9:
                return "hide_wifi_standard";
            case 10:
                return "hide_mobile_type_on_wifi";
            case 11:
                return "keep_clock_expanded";
            case 12:
                return "swap_mobile_wifi";
            default:
                throw new IllegalArgumentException();
        }
    }

    public static String setting(String str) {
        return "hypercontrol_".concat(str);
    }

    public static int signalMask() {
        boolean[] zArr = flags;
        if (zArr == null) {
            return 0;
        }
        int i = zArr[7] ? 0 | 1 : 0;
        if (zArr[8]) {
            i |= 2;
        }
        return zArr[9] ? i | 4 : i;
    }
}
