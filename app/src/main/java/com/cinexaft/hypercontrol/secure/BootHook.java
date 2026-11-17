package com.cinexaft.hypercontrol.secure;

import com.cinexaft.hypercontrol.Reflect;
import io.github.libxposed.api.XposedInterface;

/* JADX INFO: loaded from: classes.dex */
public final class BootHook implements XposedInterface.Hooker {
    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object original = chain.proceed();
        try {
            Prefs.init(chain.getThisObject());
        } catch (Throwable th) {
            Reflect.log(th);
        }
        return original;
    }
}
