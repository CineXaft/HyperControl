package dev.local.hypercontrol.core;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import dev.local.hypercontrol.State;

/* JADX INFO: loaded from: classes.dex */
public final class Config extends ContentObserver {
    public static Context context;
    private static volatile boolean[] flags = new boolean[10];
    private static Config observer;
    public static volatile boolean ready;

    public Config(Handler handler) {
        super(handler);
    }

    public static boolean enabled(int i) {
        if (ready) {
            return flags[i];
        }
        return false;
    }

    public static synchronized void init(Context context2) {
        context = context2;
        if (observer == null) {
            Config config = new Config(new Handler(Looper.getMainLooper()));
            context2.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, config);
            observer = config;
        }
        refresh();
    }

    public static String key(int i) {
        return Keys.key(i);
    }

    public static String name(int i) {
        return Keys.name(i);
    }

    public static void refresh() {
        Context context2 = context;
        if (context2 != null) {
            ContentResolver contentResolver = context2.getContentResolver();
            boolean[] zArr = new boolean[10];
            for (int i = 0; i < 10; i++) {
                boolean z = false;
                if (Settings.Global.getInt(contentResolver, State.setting(key(i)), 0) == 1) {
                    z = true;
                }
                zArr[i] = z;
            }
            flags = zArr;
            ready = true;
            Shared.policy();
            Audit.note("CONFIG ready; defaults OFF; changes observed live (shared-UID policy also requires reboot)");
            for (int i2 = 0; i2 < 10; i2++) {
                Log.i("HyperControl.Core", "CONFIG " + name(i2) + "=" + enabled(i2));
            }
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z, Uri uri) {
        String lastPathSegment;
        if (uri == null || (lastPathSegment = uri.getLastPathSegment()) == null || !lastPathSegment.startsWith("hypercontrol_system_framework_n_")) {
            return;
        }
        try {
            refresh();
        } catch (Throwable th) {
            Audit.error("CONFIG refresh failed (last valid snapshot retained)", th);
        }
    }
}
