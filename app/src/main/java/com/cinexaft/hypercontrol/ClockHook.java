package com.cinexaft.hypercontrol;

import io.github.libxposed.api.XposedInterface;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ClockHook implements XposedInterface.Hooker {
    private final int kind;

    public ClockHook(int kind) {
        this.kind = kind;
    }

    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (!State.enabled(11)) {
            return chain.proceed();
        }
        try {
            switch (this.kind) {
                case 0:
                    ClockExpand.fHasNotif.setBoolean(chain.getThisObject(), false);
                    List<?> args = chain.getArgs();
                    if (!Boolean.FALSE.equals(args.get(0))) {
                        return chain.proceed(Hooks.replaceArg(args, 0, Boolean.FALSE));
                    }
                    break;
                case 1:
                    List<?> aodArgs = chain.getArgs();
                    if (!Boolean.FALSE.equals(aodArgs.get(1))) {
                        return chain.proceed(Hooks.replaceArg(aodArgs, 1, Boolean.FALSE));
                    }
                    break;
                case 2:
                    Object result = chain.proceed();
                    clearNotificationFlags(chain.getArgs().get(0));
                    return result;
                case 3:
                    return ClockExpand.noNotifY(chain.getArgs().get(0));
                case 4:
                    Float y = ClockExpand.noNotifY(chain.getThisObject());
                    if (!y.equals(chain.getArgs().get(0))) {
                        return chain.proceed(new Object[]{y});
                    }
                    break;
                default:
                    break;
            }
        } catch (Throwable error) {
            Reflect.log(error);
        }
        return chain.proceed();
    }

    /**
     * Clears the three "notification present" flags the clock layout reads.
     *
     * <p>Each field is optional: a firmware that dropped the consistency monitor or the injector
     * still gets the flags that do exist cleared, instead of losing all three to one
     * NoSuchField/NPE.
     */
    private static void clearNotificationFlags(Object target) {
        if (target == null) {
            return;
        }
        try {
            Object monitor = ClockExpand.fMonitor == null ? null : ClockExpand.fMonitor.get(target);
            if (monitor != null && ClockExpand.fMonitorNotif != null) {
                ClockExpand.fMonitorNotif.setBoolean(monitor, false);
            }
        } catch (Throwable ignored) {
            // optional flag
        }
        try {
            Object injector = ClockExpand.fInjector == null ? null : ClockExpand.fInjector.get(target);
            Object view = injector == null || ClockExpand.fClockView == null ? null : ClockExpand.fClockView.get(injector);
            if (view != null && ClockExpand.fContainerNotif != null) {
                ClockExpand.fContainerNotif.setBoolean(view, false);
            }
        } catch (Throwable ignored) {
            // optional flag
        }
    }
}
