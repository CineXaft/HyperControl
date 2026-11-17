package com.cinexaft.hypercontrol.core;

import android.content.pm.ApplicationInfo;
import io.github.libxposed.api.XposedInterface;
import java.util.Arrays;
import java.util.Objects;

/* JADX INFO: loaded from: classes.dex */
public final class NHook implements XposedInterface.Hooker {
    private volatile boolean disabled;
    private final int fid;
    private final int kind;
    private final String label;

    public NHook(int i, int i2, String str) {
        this.fid = i;
        this.kind = i2;
        this.label = str;
    }

    private void fail(Throwable th) {
        this.disabled = true;
        Audit.error("DISABLED reflection/override failure: ".concat(this.label), th);
    }

    private void hit() {
        Audit.hit(this.fid, this.label);
    }

    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (this.disabled) {
            return chain.proceed();
        }
        if (Engine.guarding.get() != 0 && Boolean.TRUE.equals(Engine.origin.get())) {
            return chain.proceed();
        }
        int i = this.kind;
        if (!Config.enabled(this.fid)) {
            if (i == 11) {
                Engine.keyContext.set(Boolean.FALSE);
            }
            return chain.proceed();
        }
        boolean afterOriginal = false;
        boolean recover = false;
        try {
            switch (i) {
                case 0:
                    hit();
                    return null;
                case 1:
                    hit();
                    return Boolean.TRUE;
                case 2:
                    hit();
                    return Boolean.FALSE;
                case 3:
                    hit();
                    return Integer.valueOf(0);
                case 4:
                    byte[] bytes = (byte[]) chain.getArg(0);
                    if (bytes.length >= 32) {
                        byte[] digest = Arrays.copyOfRange(bytes, 0, 32);
                        hit();
                        return digest;
                    }
                    break;
                case 5:
                    int capability = ((Integer) chain.getArg(1)).intValue();
                    if (capability != 4 && capability != 16) {
                        hit();
                        return Boolean.TRUE;
                    }
                    break;
                case 6:
                    if (Config.enabled(1) && R.stack("verifySignatures")) {
                        hit();
                        return Boolean.TRUE;
                    }
                    break;
                case 7:
                    if ((((ApplicationInfo) chain.getThisObject()).flags & 129) != 0) {
                        hit();
                        return Boolean.TRUE;
                    }
                    break;
                case 8:
                    Object target = chain.getThisObject();
                    R.set(target, "mInstallFlags", Integer.valueOf(((Integer) R.get(target, "mInstallFlags")).intValue() | 524288));
                    hit();
                    break;
                case 9:
                    afterOriginal = true;
                    break;
                case 10:
                    recover = !Config.enabled(5);
                    break;
                case 11:
                    if (R.stackAny4("preparePackage", "reconcileInstallPackages", "preparePackageLI", "installPackageLI")) {
                        Engine.keyContext.set(Boolean.TRUE);
                        hit();
                        return Boolean.TRUE;
                    }
                    Engine.keyContext.set(Boolean.FALSE);
                    break;
                case 12:
                    if (Boolean.TRUE.equals(Engine.keyContext.get())) {
                        hit();
                        return Boolean.TRUE;
                    }
                    break;
                case 13:
                    afterOriginal = Config.enabled(1);
                    break;
                case 14:
                    recover = true;
                    break;
                case 15:
                case 16:
                    if (Config.enabled(1) && Shared.update(chain.getThisObject(), chain.getArg(0), i == 16)) {
                        hit();
                    }
                    break;
                default:
                    break;
            }
        } catch (Throwable throwable) {
            fail(throwable);
        }
        // The smali invokes the original outside the override-failure handler.
        if (afterOriginal) {
            Object result = chain.proceed();
            try {
                if (i == 9) {
                    R.set(chain.getThisObject(), "signatureSchemeRollbackProtectionsEnforced", Boolean.FALSE);
                    hit();
                } else if (Boolean.FALSE.equals(result)
                        && Objects.equals(R.noArgs(chain.getArg(1), "getPackageName"), chain.getArg(0))) {
                    result = Boolean.TRUE;
                    hit();
                }
            } catch (Throwable throwable) {
                fail(throwable);
            }
            return result;
        }
        if (recover) {
            Object[] outcome = new Object[2];
            try {
                outcome[0] = chain.proceed();
            } catch (Throwable throwable) {
                outcome[1] = throwable;
            }
            try {
                if (i == 10) {
                    Recovery.bytes(chain, outcome);
                } else {
                    Recovery.v1(chain, outcome);
                }
            } catch (Throwable throwable) {
                fail(throwable);
            }
            if (outcome[1] != null) {
                throw (Throwable) outcome[1];
            }
            return outcome[0];
        }
        return chain.proceed();
    }
}
