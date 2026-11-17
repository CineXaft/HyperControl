package com.cinexaft.hypercontrol.core;

import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public final class Audit {
    private static final int[] hits = new int[10];

    public static void error(String str, Throwable th) {
        Log.e("HyperControl.Core", str, th);
    }

    public static synchronized void hit(int i, String str) {
        int[] iArr = hits;
        int i2 = iArr[i] + 1;
        iArr[i] = i2;
        if (((i2 - 1) & i2) == 0) {
            note("HIT " + Config.name(i) + " #" + i2 + " " + str);
        }
    }

    public static void hooked(int i, String str) {
        note("HOOK ".concat(str));
    }

    public static void note(String str) {
        Log.i("HyperControl.Core", str);
    }
}
