package com.cinexaft.hypercontrol.hook.systemframework.core;

import android.util.Log; import com.cinexaft.hypercontrol.base.Entry.Keys; import java.util.concurrent.atomic.AtomicIntegerArray;

final class Audit {
    private static final String TAG = "HyperControl.Core";
    // Sample at powers of two without locking hook callers.
    private static final AtomicIntegerArray hits = new AtomicIntegerArray(10);
    static void error(String message, Throwable error) {
        Log.e(TAG, message, error);
    }
    static void hit(int feature, String label) {
        int count = hits.incrementAndGet(feature);
        if (((count - 1) & count) == 0) {
            note("HIT " + Keys.name(feature) + " #" + count + " " + label);
        }
    }
    static void note(String message) {
        Log.i(TAG, message);
    }
}
