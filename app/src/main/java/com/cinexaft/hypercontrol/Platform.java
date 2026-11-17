package com.cinexaft.hypercontrol;

import android.os.SystemProperties;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public final class Platform {
    public static boolean clockUnsupported() {
        try {
            String str = SystemProperties.get("ro.mi.os.version.name", "");
            if (str.startsWith("OS2.") || str.startsWith("OS3.")) {
                return true;
            }
            String str2 = SystemProperties.get("ro.build.version.incremental", "");
            return str2.startsWith("OS2.") || str2.startsWith("OS3.");
        } catch (Throwable th) {
            return false;
        }
    }

    public static int id(View view, String str, int[] iArr, int i) {
        int i2 = iArr[i];
        if (i2 != 0) {
            return i2;
        }
        int identifier = view.getResources().getIdentifier(str, "id", "com.android.systemui");
        if (identifier == 0) {
            identifier = -1;
        }
        iArr[i] = identifier;
        return identifier;
    }
}
