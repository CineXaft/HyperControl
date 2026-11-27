package sun.misc;

import java.lang.reflect.Field;

/** Compile-only API surface for the hidden/runtime Unsafe calls used by recovered passkey code. */
public abstract class Unsafe {
    public static final Unsafe theUnsafe = null;
    public abstract long objectFieldOffset(Field field);
    public abstract int getInt(Object object, long offset);
    public abstract void putInt(Object object, long offset, int value);
    public abstract void putBoolean(Object object, long offset, boolean value);
    public abstract void putObject(Object object, long offset, Object value);
    public abstract void putObjectVolatile(Object object, long offset, Object value);
}
