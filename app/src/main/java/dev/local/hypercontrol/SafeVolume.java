package dev.local.hypercontrol;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import dev.local.hypercontrol.core.R;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/* JADX INFO: loaded from: classes.dex */
public final class SafeVolume implements XposedInterface.Hooker {
    private static volatile boolean bound;
    private static volatile boolean enabled;
    private static int hits;
    private static boolean installed;
    private final int kind;

    public SafeVolume(int i) {
        this.kind = i;
    }

    private static void bind(Object obj) {
        if (bound) {
            return;
        }
        try {
            Context context = (Context) R.get(obj, "mContext");
            if (context == null) {
                throw new IllegalStateException("SoundDoseHelper.mContext is null");
            }
            boolean z = Settings.Global.getInt(context.getContentResolver(), "hypercontrol_system_framework_safe_volume", 0) == 1;
            enabled = z;
            bound = true;
            Log.i("HyperControl.Volume", "CONFIG enabled=" + z + "; read before audio threshold initialization");
        } catch (Throwable th) {
            Log.e("HyperControl.Volume", "CONFIG failed; keeping original threshold", th);
        }
    }

    private static void deopt(Executable executable) {
        try {
            if (Entry.module.deoptimize(executable)) {
                return;
            }
            Log.w("HyperControl.Volume", "WARN deoptimization returned false: ".concat(executable.toString()));
        } catch (Throwable th) {
            Log.w("HyperControl.Volume", "caller deoptimization failed", th);
        }
    }

    private static synchronized void hit() {
        int i = hits + 1;
        hits = i;
        if (((i - 1) & i) == 0) {
            Log.i("HyperControl.Volume", "HIT updateSafeMediaVolumeIndex(int) -> 0x7fffffff #" + i);
        }
    }

    private static void hook(Method method, int i) {
        method.setAccessible(true);
        Reflect.hook(method, new SafeVolume(i));
        Log.i("HyperControl.Volume", "HOOK ".concat(method.toString()));
    }

    public static synchronized void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        try {
            Class<?> clsLoadClass = classLoader.loadClass("com.android.server.audio.SoundDoseHelperStubImpl");
            Class<?> cls = Integer.TYPE;
            Method declaredMethod = clsLoadClass.getDeclaredMethod("updateSafeMediaVolumeIndex", cls);
            if (declaredMethod.getReturnType() == cls && !Modifier.isStatic(declaredMethod.getModifiers())) {
                Class<?> clsLoadClass2 = classLoader.loadClass("com.android.server.audio.SoundDoseHelper");
                Method declaredMethod2 = clsLoadClass2.getDeclaredMethod("initSafeVolumes", new Class[0]);
                if (declaredMethod2.getReturnType() == Void.TYPE) {
                    hook(declaredMethod2, 1);
                    hook(declaredMethod, 0);
                    installed = true;
                    deopt(declaredMethod);
                    deopt(declaredMethod2);
                    for (Constructor<?> constructor : clsLoadClass2.getDeclaredConstructors()) {
                        deopt(constructor);
                    }
                    for (Method method : clsLoadClass2.getDeclaredMethods()) {
                        if ("onConfigureSafeMedia".equals(method.getName())) {
                            deopt(method);
                        }
                    }
                    return;
                }
            }
            throw new NoSuchMethodException("safe-volume hook signatures changed");
        } catch (Throwable th) {
            Log.e("HyperControl.Volume", "INSTALL failed", th);
        }
    }

    public Object intercept(XposedInterface.Chain chain) {
        if (this.kind != 0) {
            bind(chain.getThisObject());
        } else if (enabled) {
            hit();
            return Integer.MAX_VALUE;
        }
        return chain.proceed();
    }
}
