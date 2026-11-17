package com.cinexaft.hypercontrol;

import io.github.libxposed.api.XposedInterface;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class ClockHook implements XposedInterface.Hooker {
    private final int kind;

    public ClockHook(int i) {
        this.kind = i;
    }

    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object obj;
        if (State.enabled(11)) {
            try {
                switch (this.kind) {
                    case 0:
                        ClockExpand.fHasNotif.setBoolean(chain.getThisObject(), false);
                        List args = chain.getArgs();
                        Object obj2 = args.get(0);
                        Boolean bool = Boolean.FALSE;
                        if (!bool.equals(obj2)) {
                            return chain.proceed(new Object[]{bool, args.get(1)});
                        }
                        break;
                    case 1:
                        List args2 = chain.getArgs();
                        Object obj3 = args2.get(1);
                        Boolean bool2 = Boolean.FALSE;
                        if (!bool2.equals(obj3)) {
                            Object[] array = args2.toArray(new Object[5]);
                            array[1] = bool2;
                            return chain.proceed(array);
                        }
                        break;
                    case 2:
                        Object objProceed = chain.proceed();
                        Object obj4 = chain.getArgs().get(0);
                        Object obj5 = ClockExpand.fMonitor.get(obj4);
                        if (obj5 != null) {
                            ClockExpand.fMonitorNotif.setBoolean(obj5, false);
                        }
                        Object obj6 = ClockExpand.fInjector.get(obj4);
                        if (obj6 != null && (obj = ClockExpand.fClockView.get(obj6)) != null) {
                            ClockExpand.fContainerNotif.setBoolean(obj, false);
                        }
                        return objProceed;
                    case 3:
                        return ClockExpand.noNotifY(chain.getArgs().get(0));
                    case 4:
                        Float fNoNotifY = ClockExpand.noNotifY(chain.getThisObject());
                        if (!fNoNotifY.equals(chain.getArgs().get(0))) {
                            return chain.proceed(new Object[]{fNoNotifY});
                        }
                        break;
                }
            } catch (Throwable th) {
                Reflect.log(th);
            }
        }
        return chain.proceed();
    }
}
