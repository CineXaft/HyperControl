package android.os;

/** Compile-only stand-in for Android's hidden SystemProperties API. Never packaged. */
public final class SystemProperties {
    public static String get(String key, String defaultValue) { throw new UnsupportedOperationException("compile-only stub"); }
    private SystemProperties() { }
}
