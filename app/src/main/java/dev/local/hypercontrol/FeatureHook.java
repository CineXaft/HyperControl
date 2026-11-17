package dev.local.hypercontrol;

import android.content.Context;
import io.github.libxposed.api.XposedInterface;
import java.io.IOException;

/* JADX INFO: loaded from: classes.dex */
public final class FeatureHook implements XposedInterface.Hooker {
    private final int mode;

    public FeatureHook(int i) {
        this.mode = i;
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        int i = this.mode;
        if (i < 0) {
            Object arg = chain.getArg(0);
            if (arg instanceof Context) {
                State.bind((Context) arg);
            }
        } else if (i == 10) {
            String strCustomMobileText = State.customMobileText();
            if (strCustomMobileText != null) {
                return strCustomMobileText;
            }
        } else if (i != 7) {
            if (State.enabled(i)) {
                if (i == 1 || i == 3) {
                    return Boolean.FALSE;
                }
                return null;
            }
        } else if (State.enabled(6)) {
            Object arg2 = chain.getArg(0);
            if ((arg2 instanceof String) && ((String) arg2).contains(".xlDownload")) {
                throw new IOException(".xlDownload is blocked");
            }
        }
        return chain.proceed();
    }
}
