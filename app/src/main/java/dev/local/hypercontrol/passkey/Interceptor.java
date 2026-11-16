package dev.local.hypercontrol.passkey;

import android.content.ComponentName;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Interceptor implements XposedInterface.Hooker {
    private final Field field;
    private final int kind;

    public Interceptor(int i, Field field) {
        this.kind = i;
        this.field = field;
    }

    private static boolean blockReset(List list) {
        if (list.size() < 3) {
            return false;
        }
        Object obj = list.get(1);
        if (!"autofill_service".equals(obj) && !"credential_service".equals(obj) && !"credential_service_primary".equals(obj)) {
            return false;
        }
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            String className = stackTraceElement.getClassName();
            if ("com.miui.securitycenter.Application".equals(className) || "com.miui.securitycenter.service.CacheService".equals(className)) {
                return true;
            }
        }
        return false;
    }

    private static synchronized Object international(Field field, XposedInterface.Chain chain) throws Throwable {
        if (field != null) {
            Object original;
            try {
                original = field.get(null);
                Fields.write(field, null, Boolean.TRUE);
            } catch (Throwable th) {
                Policy.warn("MIUI regional flag unavailable; retaining original behavior", th);
                return chain.proceed();
            }
            // Only flag acquisition/update is caught above. Original-method and
            // restoration failures must propagate, without proceeding a second time.
            try {
                return chain.proceed();
            } finally {
                Fields.write(field, null, original);
            }
        }
        return chain.proceed();
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (Policy.enabled()) {
            int i = this.kind;
            if (i == 0) {
                return international(this.field, chain);
            }
            if (i == 1) {
                Object objProceed = chain.proceed();
                try {
                    Fields.write(this.field, chain.getThisObject(), "com.google.android.gms/.auth.api.credentials.credman.service.RemoteService");
                    return objProceed;
                } catch (Throwable th) {
                    Policy.warn("Hybrid credential provider update failed", th);
                    return objProceed;
                }
            }
            if (i == 2) {
                try {
                    ComponentName componentNameComponent = Chooser.component(chain.getArgs());
                    if (componentNameComponent != null) {
                        return componentNameComponent;
                    }
                } catch (Throwable th2) {
                    Policy.warn("Google credential chooser unavailable; retaining system chooser", th2);
                }
            } else {
                if (i == 3) {
                    return "";
                }
                if (i != 4) {
                    Object objProceed2 = chain.proceed();
                    try {
                        SwitchBinding.bind(chain.getThisObject(), chain.getArgs().get(0));
                        return objProceed2;
                    } catch (Throwable th3) {
                        Policy.warn("Credential preference binding skipped", th3);
                        return objProceed2;
                    }
                }
                if (blockReset(chain.getArgs())) {
                    return Boolean.TRUE;
                }
            }
        }
        return chain.proceed();
    }
}
