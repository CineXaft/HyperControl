package dev.local.hypercontrol.secure;

import dev.local.hypercontrol.Reflect;
import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class BootHook implements XposedInterface.Hooker {
    public Object intercept(XposedInterface.Chain chain) {
        Object objProceed = chain.proceed();
        try {
            Prefs.init(chain.getThisObject());
            return objProceed;
        } catch (Throwable th) {
            Reflect.log(th);
            return objProceed;
        }
    }
}
