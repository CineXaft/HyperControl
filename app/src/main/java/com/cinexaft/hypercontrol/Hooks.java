package com.cinexaft.hypercontrol;

import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Shared hook plumbing.
 *
 * <p>Every part of the module needs the same four things: find a target method on an unknown
 * firmware, install a hooker, rewrite one argument, and log. Keeping that here means the hook
 * classes contain only their actual decision logic, and a signature change is handled in one
 * place instead of in each of them.
 */
public final class Hooks {

    /** Tag used for the one-line "hooked ..." diagnostics. */
    public static final String TAG = "HyperControl";

    private Hooks() {
    }

    /**
     * Resolves a declared method by name and parameter count.
     *
     * <p>Parameter *types* differ between MIUI releases (a {@code ZZ} call becomes {@code ZZZ}),
     * but the name and arity have been stable, so arity matching is what makes a hook survive a
     * firmware bump. Returns {@code null} instead of throwing so a missing target only disables
     * that one hook.
     */
    public static Method byNameCount(Class<?> owner, String name, int count) {
        if (owner == null) {
            return null;
        }
        for (Method method : owner.getDeclaredMethods()) {
            if (name.equals(method.getName()) && method.getParameterTypes().length == count) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }

    /**
     * Resolves a method by exact signature and verifies its return type.
     *
     * <p>Used where the caller must not guess: if the return type changed, the override would
     * throw on every call, so the caller skips the hook instead. Returns {@code null} when the
     * class, the method or the return type does not match.
     */
    public static Method bySignature(ClassLoader loader, String className, String name,
                                     Class<?>[] params, Class<?> returnType) {
        try {
            Method method = loader.loadClass(className).getDeclaredMethod(name, params);
            if (method.getReturnType() != returnType) {
                return null;
            }
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Deoptimizes, hooks and logs a target in one step; never throws. */
    public static void install(Method method, XposedInterface.Hooker hooker) {
        install(method, hooker, TAG);
    }

    public static void install(Method method, XposedInterface.Hooker hooker, String tag) {
        if (method == null) {
            return;
        }
        try {
            if (Entry.module != null) {
                Entry.module.deoptimize(method);
            }
            Reflect.hook(method, hooker);
            Log.i(tag, method.toString());
        } catch (Throwable error) {
            Reflect.log(error);
        }
    }

    /**
     * Copies the invocation arguments with one slot replaced.
     *
     * <p>The original code built these arrays by hand at every call site; {@code toArray} keeps
     * the copy correct even if a firmware adds trailing parameters.
     */
    public static Object[] replaceArg(List<?> args, int index, Object value) {
        Object[] copy = args.toArray(new Object[args.size()]);
        copy[index] = value;
        return copy;
    }
}
