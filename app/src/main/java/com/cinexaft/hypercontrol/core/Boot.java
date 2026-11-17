package com.cinexaft.hypercontrol.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class Boot implements Runnable, XposedInterface.Hooker {
    private int attempts;
    private Context context;

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object objProceed = chain.proceed();
        try {
            this.context = (Context) R.get(chain.getThisObject(), "mSystemContext");
            new Handler(Looper.getMainLooper()).post(this);
            Audit.note("BOOT services returned; configuration read queued");
            return objProceed;
        } catch (Throwable th) {
            Audit.error("BOOT context acquisition", th);
            return objProceed;
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            Context context = this.context;
            if (context != null) {
                Config.init(context);
            }
        } catch (Throwable th) {
            int i = this.attempts + 1;
            this.attempts = i;
            Audit.error("BOOT config unavailable; remaining original behavior until ready", th);
            if (i < 20) {
                new Handler(Looper.getMainLooper()).postDelayed(this, 1500L);
            }
        }
    }
}
