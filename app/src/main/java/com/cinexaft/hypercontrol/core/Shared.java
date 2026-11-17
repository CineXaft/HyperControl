package com.cinexaft.hypercontrol.core;

import java.lang.reflect.Field;

/* JADX INFO: loaded from: classes.dex */
public final class Shared {
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }

    private static boolean policyOriginal;
    private static boolean policySaved;

    private static boolean capable(Object obj, Object obj2) {
        ThreadLocal<Object> previous = Engine.origin;
        Object saved = previous.get();
        previous.set(Boolean.TRUE);
        Engine.guarding++;
        try {
            return ((Boolean) R.call(obj, "checkCapability", new Object[]{obj2, 0})).booleanValue();
        } finally {
            Engine.guarding--;
            previous.set(saved);
        }
    }

    public static Object details(Object obj) {
        Object obj2;
        if (obj == null || (obj2 = R.get(obj, "signatures")) == null) {
            return null;
        }
        return R.get(obj2, "mSigningDetails");
    }

    public static synchronized void policy() {
        try {
            ClassLoader classLoader = Engine.loader;
            if (classLoader != null) {
                Class<?> clsLoadClass = classLoader.loadClass("com.android.server.pm.ReconcilePackageUtils");
                Field field = R.field(clsLoadClass, "ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS");
                boolean z = field.getBoolean(null);
                if (!policySaved) {
                    policyOriginal = z;
                    policySaved = true;
                }
                boolean z2 = (Config.enabled(1) && Config.enabled(3)) ? true : policyOriginal;
                if (z != z2) {
                    try {
                        field.setBoolean(null, z2);
                    } catch (IllegalAccessException e) {
                        int i = R.field(Field.class, "offset").getInt(field);
                        if (i <= 0) {
                            throw new IllegalStateException("unsupported Field.offset; not guessing a native memory offset");
                        }
                        R.call(R.field(Class.forName("sun.misc.Unsafe"), "theUnsafe").get(null), "putBoolean", new Object[]{clsLoadClass, Long.valueOf(i), Boolean.valueOf(z2)});
                    }
                    if (field.getBoolean(null) != z2) {
                        throw new IllegalStateException("shared UID policy write did not stick");
                    }
                    Engine.deopt("com.android.server.pm.ReconcilePackageUtils", "reconcilePackages");
                }
                Audit.note("POLICY ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS=" + z2);
            }
        } catch (Throwable th) {
            Audit.error("shared UID policy", th);
        }
    }

    public static boolean update(Object obj, Object obj2, boolean z) {
        if (!Config.enabled(1) || !Config.enabled(3) || obj == null || obj2 == null
                || (((Integer) R.get(obj, "uidFlags")).intValue() & 1) != 0) {
            return false;
        }
        Object originalDetails = details(obj);
        if (originalDetails == null) {
            return false;
        }
        Object packages = R.get(obj, "mPackages");
        if (packages == null) {
            return false;
        }
        try {
            Object storage = R.get(packages, "mStorage");
            if (storage != null) {
                packages = storage;
            }
        } catch (Throwable throwable) {
            // Only a missing optional mStorage field is ignored in the smali.
            if (!(throwable instanceof NoSuchFieldException)) {
                Shared.<RuntimeException>sneakyThrow(throwable);
            }
        }

        int size = ((Integer) R.noArgs(packages, "size")).intValue();
        boolean foundTarget = false;
        Object mergedDetails = null;
        for (int index = 0; index < size; index++) {
            Object packageSetting = R.call(packages, "valueAt", new Object[]{Integer.valueOf(index)});
            if (packageSetting == null) {
                continue;
            }
            if (packageSetting.equals(obj2)) {
                foundTarget = true;
                if (z) {
                    continue;
                }
                packageSetting = obj2;
            }
            Object candidateDetails = details(packageSetting);
            if (candidateDetails == null) {
                continue;
            }
            if (capable(candidateDetails, originalDetails)) {
                return false;
            }
            if (capable(originalDetails, candidateDetails)) {
                return false;
            }
            mergedDetails = mergedDetails == null
                    ? candidateDetails
                    : R.call(mergedDetails, "mergeLineageWith",
                            new Object[]{candidateDetails, Integer.valueOf(2)});
        }
        if (!foundTarget || mergedDetails == null) {
            return false;
        }
        R.set(R.get(obj, "signatures"), "mSigningDetails", mergedDetails);
        return true;
    }
}
