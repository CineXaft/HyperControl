package dev.local.hypercontrol.securitycenter;

import android.content.Context;
import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class SecurityHook implements XposedInterface.Hooker {
    private final int mode;

    public SecurityHook(int i) {
        this.mode = i;
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        int i = this.mode;
        if (i == 0 || i == 1) {
            if (SecurityCenter.enabled(i)) {
                return 0;
            }
        } else if (i == 2) {
            if (SecurityCenter.enabled(2)) {
                SecurityCenter.setAdbActive(true);
                try {
                    return chain.proceed();
                } finally {
                    SecurityCenter.setAdbActive(false);
                }
            }
        } else if (i == 6) {
            if (SecurityCenter.enabled(0)) {
                Object arg = chain.getArg(1);
                if ((arg instanceof String) && ((String) arg).contains("getMinusPredictScore")) {
                    for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                        if ("com.miui.securityscan.scanner.ScoreManager".equals(stackTraceElement.getClassName())) {
                            SecurityCenter.discoverScore(stackTraceElement.getMethodName());
                            break;
                        }
                    }
                }
            }
        } else if (i == 4) {
            Object arg2 = chain.getArg(0);
            if (arg2 instanceof Context) {
                SecurityCenter.bind((Context) arg2);
            }
        } else if (i == 5 && SecurityCenter.adbActive()) {
            Object arg3 = chain.getArg(0);
            if (arg3 instanceof String && SecurityCenter.isBypassString((String) arg3)) {
                return Boolean.TRUE;
            }
            Object thisObject = chain.getThisObject();
            if (thisObject instanceof String && SecurityCenter.isBypassString((String) thisObject)) {
                return Boolean.TRUE;
            }
        }
        return chain.proceed();
    }
}
