package dev.local.hypercontrol.secure;

import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class Callback implements XposedInterface.Hooker {
    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (FlagSecure.enabled) {
            return Boolean.FALSE;
        }
        return chain.proceed();
    }
}
