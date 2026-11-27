package com.cinexaft.hypercontrol.hook.systemframework.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reflection and live call-context checks shared by framework and Settings hooks. */
public final class R {
    public static boolean accepts(Class<?> type, Object value) {
        if (!type.isPrimitive()) return value == null || type.isInstance(value);
        if (type == int.class) return value instanceof Integer;
        if (type == boolean.class) return value instanceof Boolean;
        return type == long.class && value instanceof Long;
    }
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable error) throws T {
        throw (T) error;
    }
    public static Object call(Object target, String name, Object[] arguments) {
        try {
            for (Class<?> owner = target.getClass(); owner != null; owner = owner.getSuperclass()) {
                methods: for (Method method : owner.getDeclaredMethods()) {
                    if (!name.equals(method.getName())) continue;
                    Class<?>[] parameters = method.getParameterTypes();
                    if (parameters.length != arguments.length) continue;
                    for (int index = 0; index < parameters.length; index++) {
                        if (!accepts(parameters[index], arguments[index])) continue methods;
                    }
                    method.setAccessible(true);
                    return method.invoke(target, arguments);
                }
            }
            throw new NoSuchMethodException(target.getClass().getName() + "#" + name);
        } catch (Throwable error) {
            sneakyThrow(error);
            return null;
        }
    }
    public static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(type == null ? name : type.getName() + "#" + name);
    }
    public static Object get(Object target, String name) {
        try {
            return findField(target.getClass(), name).get(target);
        } catch (Throwable error) {
            sneakyThrow(error);
            return null;
        }
    }
    public static Object noArgs(Object target, String name) {
        return call(target, name, new Object[0]);
    }
    public static void set(Object target, String name, Object value) {
        try {
            findField(target.getClass(), name).set(target, value);
        } catch (Throwable error) {
            sneakyThrow(error);
        }
    }
    public static boolean stack(String... names) {
        // A stack belongs to this invocation, never to the lifetime of a pooled thread.
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            for (String name : names) {
                if (name.equals(frame.getMethodName())) return true;
            }
        }
        return false;
    }
}
