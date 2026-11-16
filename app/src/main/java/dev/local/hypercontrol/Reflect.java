package dev.local.hypercontrol;

import android.util.Log;
import dev.local.hypercontrol.core.R;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class Reflect {
    public static XposedInterface.HookHandle attach(Executable executable, XposedInterface.Hooker hooker, XposedInterface.ExceptionMode exceptionMode) {
        return Entry.module.hook(executable).setExceptionMode(exceptionMode).intercept(hooker);
    }

    public static Field field(Object obj, String str) {
        return R.field(obj.getClass(), str);
    }

    public static Object getObjectField(Object obj, String str) {
        return R.get(obj, str);
    }

    public static void hook(Method method, XposedInterface.Hooker hooker) {
        attach(method, hooker, XposedInterface.ExceptionMode.PROTECTIVE);
    }

    public static void log(Throwable th) {
        Log.e("HyperControl", "Hook error", th);
    }

    public static void setIntField(Object obj, String str, int i) throws IllegalAccessException {
        field(obj, str).setInt(obj, i);
    }

    public static void setLongField(Object obj, String str, long j) throws IllegalAccessException {
        field(obj, str).setLong(obj, j);
    }

    public static void setObjectField(Object obj, String str, Object obj2) {
        R.set(obj, str, obj2);
    }
}
