package dev.local.hypercontrol.secure;

import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class Callback implements XposedInterface.Hooker {
    public Object intercept(XposedInterface.Chain chain) {
        return FlagSecure.enabled ? Boolean.FALSE : chain.proceed();
    }
}
