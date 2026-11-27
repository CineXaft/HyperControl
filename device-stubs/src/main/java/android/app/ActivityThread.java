package android.app;

/** Compile-only stand-in for Android hidden APIs. Never package or run this class. */
public final class ActivityThread {
    public static Application currentApplication() { throw new UnsupportedOperationException("compile-only stub"); }
    public static ActivityThread currentActivityThread() { throw new UnsupportedOperationException("compile-only stub"); }
    public ContextImpl getSystemContext() { throw new UnsupportedOperationException("compile-only stub"); }
}
