package dev.local.hypercontrol.passkey;

import android.app.ActivityThread;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import dev.local.hypercontrol.core.Config;

/* JADX INFO: loaded from: classes.dex */
public final class Policy {
    public static boolean enabled() {
        try {
            Context contextImplCurrentApplication = ActivityThread.currentApplication();
            if (contextImplCurrentApplication == null && (contextImplCurrentApplication = Config.context) == null) {
                ActivityThread activityThreadCurrentActivityThread = ActivityThread.currentActivityThread();
                if (activityThreadCurrentActivityThread == null) {
                    return false;
                }
                contextImplCurrentApplication = activityThreadCurrentActivityThread.getSystemContext();
            }
            return Settings.Global.getInt(contextImplCurrentApplication.getContentResolver(), "hypercontrol_system_framework_passkey", 0) == 1;
        } catch (Throwable th) {
            return false;
        }
    }

    public static void warn(String str, Throwable th) {
        Log.w("HyperControl.Passkey", str, th);
    }
}
