package com.cinexaft.hypercontrol.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class R {


    public static boolean accepts(Class cls, Object obj) {
        if (!cls.isPrimitive()) {
            if (obj != null) {
                return cls.isInstance(obj);
            }
            return true;
        }
        if (cls == Integer.TYPE) {
            return obj instanceof Integer;
        }
        if (cls == Boolean.TYPE) {
            return obj instanceof Boolean;
        }
        if (cls == Long.TYPE) {
            return obj instanceof Long;
        }
        return false;
    }

    // DEX reflection helpers propagate checked failures without wrapping them.
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

    public static Object call(Object obj, String str, Object[] objArr) {
        Class<?>[] parameterTypes;
        int length;
        try {
            for (Class<?> superclass = obj.getClass(); superclass != null; superclass = superclass.getSuperclass()) {
                for (Method method : superclass.getDeclaredMethods()) {
                    if (str.equals(method.getName()) && (length = (parameterTypes = method.getParameterTypes()).length) == objArr.length) {
                        int i = 0;
                        while (true) {
                            if (i >= length) {
                                method.setAccessible(true);
                                return method.invoke(obj, objArr);
                            }
                            if (!accepts(parameterTypes[i], objArr[i])) {
                                break;
                            }
                            i++;
                        }
                    }
                }
            }
            sneakyThrow(new NoSuchMethodException(str));
        } catch (Throwable throwable) {
            sneakyThrow(throwable);
        }
        return null;
    }

    public static Field field(Class cls, String str) {
        try {
            while (cls != null) {
                try {
                    Field declaredField = cls.getDeclaredField(str);
                    declaredField.setAccessible(true);
                    return declaredField;
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                }
            }
            sneakyThrow(new NoSuchFieldException(str));
        } catch (Throwable throwable) {
            sneakyThrow(throwable);
        }
        return null;
    }

    public static Object get(Object obj, String str) {
        try {
            return field(obj.getClass(), str).get(obj);
        } catch (Throwable throwable) {
            sneakyThrow(throwable);
            return null;
        }
    }

    public static Object noArgs(Object obj, String str) {
        return call(obj, str, new Object[0]);
    }

    public static void set(Object obj, String str, Object obj2) {
        try {
            field(obj.getClass(), str).set(obj, obj2);
        } catch (Throwable throwable) {
            sneakyThrow(throwable);
        }
    }

    public static boolean stack(String str) {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (str.equals(stackTraceElement.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean stackAny4(String str, String str2, String str3, String str4) {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            String methodName = stackTraceElement.getMethodName();
            if (str.equals(methodName) || str2.equals(methodName) || str3.equals(methodName) || str4.equals(methodName)) {
                return true;
            }
        }
        return false;
    }
}
