package dev.local.hypercontrol.secure;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import dev.local.hypercontrol.Reflect;

/* JADX INFO: loaded from: classes.dex */
public final class Prefs extends ContentObserver {
    public static final String KEY = "hypercontrol_system_framework_flag_secure";
    private static boolean registered;
    private final ContentResolver resolver;

    public Prefs(Handler handler, ContentResolver contentResolver) {
        super(handler);
        this.resolver = contentResolver;
    }

    public static synchronized void init(Object obj) {
        if (registered) {
            return;
        }
        try {
            Context context = (Context) Reflect.getObjectField(obj, "mSystemContext");
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                Prefs prefs = new Prefs(new Handler(Looper.getMainLooper()), contentResolver);
                contentResolver.registerContentObserver(Settings.Global.getUriFor(KEY), false, prefs);
                registered = true;
                prefs.refresh();
            }
        } catch (Throwable th) {
            FlagSecure.enabled = false;
            Reflect.log(th);
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z) {
        refresh();
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z, Uri uri) {
        refresh();
    }

    public void refresh() {
        try {
            boolean z = Settings.Global.getInt(this.resolver, KEY, 0) == 1;
            FlagSecure.enabled = z;
            Log.i("HyperControl.FlagSecure", z ? "enabled=true; cached Settings.Global preference" : "enabled=false; original behavior");
        } catch (Throwable th) {
            FlagSecure.enabled = false;
            Reflect.log(th);
        }
    }
}
