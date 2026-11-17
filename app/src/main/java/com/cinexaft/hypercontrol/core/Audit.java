package com.cinexaft.hypercontrol.core;

import android.util.Log;
import java.util.concurrent.atomic.AtomicIntegerArray;

/* JADX INFO: loaded from: classes.dex */
public final class Audit {

    /**
     * Hit counters.
     *
     * <p>These used to be a plain {@code int[]} behind a {@code synchronized} method, which meant
     * every single interception took a global monitor. The hooks that count hits run inside
     * PackageManagerService, so that lock was the most contended thing in the module. A
     * lock-free array keeps the same "log on powers of two" behaviour without the monitor.
     */
    private static final AtomicIntegerArray hits = new AtomicIntegerArray(10);

    public static void error(String message, Throwable error) {
        Log.e("HyperControl.Core", message, error);
    }

    public static void hit(int feature, String label) {
        int count = hits.incrementAndGet(feature);
        if (((count - 1) & count) == 0) {
            note("HIT " + Config.name(feature) + " #" + count + " " + label);
        }
    }

    public static void hooked(int feature, String label) {
        note("HOOK ".concat(label));
    }

    public static void note(String message) {
        Log.i("HyperControl.Core", message);
    }
}
