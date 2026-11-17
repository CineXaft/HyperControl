package com.cinexaft.hypercontrol.passkey;

import android.graphics.Point;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/* JADX INFO: loaded from: classes.dex */
public final class Fields {
    private static Unsafe memory = null;
    private static long slot = -1;

    public static Field find(Class cls, String str) throws NoSuchFieldException {
        while (cls != null) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        throw new NoSuchFieldException(str);
    }

    private static synchronized Unsafe memory() throws NoSuchFieldException, IllegalAccessException {
        Unsafe unsafe = memory;
        if (unsafe != null) {
            return unsafe;
        }
        Field declaredField = Unsafe.class.getDeclaredField("theUnsafe");
        declaredField.setAccessible(true);
        Unsafe unsafe2 = (Unsafe) declaredField.get(null);
        memory = unsafe2;
        return unsafe2;
    }

    private static synchronized long slot() throws IllegalAccessException, NoSuchFieldException {
        long j = slot;
        if (j >= 0) {
            return j;
        }
        Unsafe unsafeMemory = memory();
        try {
            Field declaredField = Field.class.getDeclaredField("offset");
            declaredField.setAccessible(true);
            declaredField.getInt(declaredField);
            long jObjectFieldOffset = unsafeMemory.objectFieldOffset(declaredField);
            slot = jObjectFieldOffset;
            return jObjectFieldOffset;
        } catch (Exception e) {
            Field declaredField2 = Point.class.getDeclaredField("x");
            declaredField2.getInt(new Point());
            int iObjectFieldOffset = (int) unsafeMemory.objectFieldOffset(declaredField2);
            for (int i = 8; i < 256; i += 4) {
                long j2 = i;
                if (unsafeMemory.getInt(declaredField2, j2) == iObjectFieldOffset) {
                    int i2 = ~iObjectFieldOffset;
                    unsafeMemory.putInt(declaredField2, j2, i2);
                    long jObjectFieldOffset2;
                    try {
                        jObjectFieldOffset2 = unsafeMemory.objectFieldOffset(declaredField2);
                    } catch (Throwable th) {
                        unsafeMemory.putInt(declaredField2, j2, iObjectFieldOffset);
                        throw th;
                    }
                    unsafeMemory.putInt(declaredField2, j2, iObjectFieldOffset);
                    if (((int) jObjectFieldOffset2) == i2) {
                        slot = j2;
                        return j2;
                    }
                }
            }
            throw new NoSuchFieldException("ART field offset unavailable");
        }
    }

    public static void write(Field field, Object obj, Object obj2) throws IllegalAccessException, NoSuchFieldException {
        field.setAccessible(true);
        try {
            field.set(obj, obj2);
        } catch (IllegalAccessException e) {
            field.get(obj);
            Unsafe unsafeMemory = memory();
            long j = unsafeMemory.getInt(field, slot());
            Object declaringClass = (field.getModifiers() & 8) != 0 ? field.getDeclaringClass() : obj;
            if (field.getType() == Boolean.TYPE) {
                unsafeMemory.putBoolean(declaringClass, j, ((Boolean) obj2).booleanValue());
            } else {
                unsafeMemory.putObjectVolatile(declaringClass, j, obj2);
            }
        }
    }
}
