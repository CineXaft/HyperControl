package com.cinexaft.hypercontrol.hook.systemframework.core;

import android.content.Context; import android.database.ContentObserver; import android.net.Uri; import android.os.Handler; import android.os.Looper; import android.provider.Settings;
import com.cinexaft.hypercontrol.base.Entry.Keys; import com.cinexaft.hypercontrol.base.Entry.State; import io.github.libxposed.api.XposedInterface;

/** Loads the core settings at boot and keeps their snapshot up to date. */
public final class Config extends ContentObserver {
    private static final int FEATURE_COUNT = 10;
    private static final String PREFIX = "hypercontrol_system_framework_n_";
    public static Context context;
    private static Config observer;
    private static volatile long flags;
    private static volatile boolean ready;

    private Config(Handler handler) {
        super(handler);
    }
    static boolean enabled(int feature) {
        return ready && (flags & (1L << feature)) != 0;
    }
    private static synchronized void init(Context systemContext, Handler handler) {
        context = systemContext;
        if (observer == null) {
            var config = new Config(handler);
            systemContext.getContentResolver().registerContentObserver(Settings.Global.CONTENT_URI, true, config);
            observer = config;
        }
        refresh();
    }
    private static void refresh() {
        Context systemContext = context;
        if (systemContext == null) return;
        var resolver = systemContext.getContentResolver();
        long updated = 0L;
        for (int feature = 0; feature < FEATURE_COUNT; feature++) {
            if (Settings.Global.getInt(resolver, State.setting(Keys.key(feature)), 0) == 1) {
                updated |= 1L << feature;
            }
        }
        flags = updated;
        ready = true;
        Shared.policy();
        Audit.note("CONFIG ready; defaults OFF; changes observed live (shared-UID policy also requires reboot)");
        for (int feature = 0; feature < FEATURE_COUNT; feature++) {
            Audit.note("CONFIG " + Keys.name(feature) + "=" + enabled(feature));
        }
    }
    @Override
    public void onChange(boolean selfChange, Uri uri) {
        String key = uri == null ? null : uri.getLastPathSegment();
        if (key != null && !key.startsWith(PREFIX)) return;
        try {
            refresh();
        } catch (Throwable error) {
            Audit.error("CONFIG refresh failed (last valid snapshot retained)", error);
        }
    }
    static final class Boot implements Runnable, XposedInterface.Hooker {
        private Context systemContext;
        private Handler handler;
        private int attempts;
        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            Object result = chain.proceed();
            try {
                systemContext = (Context) R.get(chain.getThisObject(), "mSystemContext");
                if (handler == null) handler = new Handler(Looper.getMainLooper());
                handler.post(this);
                Audit.note("BOOT services returned; configuration read queued");
            } catch (Throwable error) {
                Audit.error("BOOT context acquisition", error);
            }
            return result;
        }
        @Override
        public void run() {
            try {
                if (systemContext != null) init(systemContext, handler);
            } catch (Throwable error) {
                Audit.error("BOOT config unavailable; remaining original behavior until ready", error);
                if (++attempts < 20) handler.postDelayed(this, 1500L);
            }
        }
    }
}
