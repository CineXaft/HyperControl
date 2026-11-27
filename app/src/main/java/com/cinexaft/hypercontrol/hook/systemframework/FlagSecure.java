package com.cinexaft.hypercontrol.hook.systemframework;

import android.content.ContentResolver; import android.content.Context; import android.database.ContentObserver; import android.net.Uri; import android.os.Handler; import android.os.Looper;
import android.provider.Settings; import android.util.Log; import com.cinexaft.hypercontrol.base.Entry.State; import com.cinexaft.hypercontrol.base.SettingsHook.Reflect; import com.cinexaft.hypercontrol.hook.systemframework.core.R; import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Method;

public final class FlagSecure {
    public static volatile boolean enabled;
    private static boolean installed;
    private static void attach(Method method) {
        if (method.getReturnType() != Boolean.TYPE) {
            throw new IllegalArgumentException("FlagSecure target must return boolean");
        }
        method.setAccessible(true);
        Reflect.hook(method, new Callback());
        Log.i("HyperControl.FlagSecure", method.toString());
    }
    public static synchronized void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        installed = true;
        try {
            Method declaredMethod = classLoader.loadClass("com.android.server.SystemServer").getDeclaredMethod("startOtherServices", classLoader.loadClass("com.android.server.utils.TimingsTraceAndSlog"));
            declaredMethod.setAccessible(true);
            Reflect.hook(declaredMethod, new BootHook());
        } catch (Throwable th) {
            Reflect.log(th);
        }
        try {
            attach(classLoader.loadClass("com.android.server.wm.WindowState").getDeclaredMethod("isSecureLocked", new Class[0]));
        } catch (Throwable th2) {
            Reflect.log(th2);
        }
        try {
            attach(classLoader.loadClass("com.android.server.wm.WindowManagerServiceImpl").getDeclaredMethod("notAllowCaptureDisplay", classLoader.loadClass("com.android.server.wm.RootWindowContainer"), Integer.TYPE));
        } catch (Throwable th3) {
            Reflect.log(th3);
        }
    }
}
final class BootHook implements XposedInterface.Hooker {
    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        Object original = chain.proceed();
        try {
            Prefs.init(chain.getThisObject());
        } catch (Throwable th) {
            Reflect.log(th);
        }
        return original;
    }
}
final class Callback implements XposedInterface.Hooker {
    @Override
    public Object intercept(XposedInterface.Chain chain) throws Throwable {
        if (FlagSecure.enabled) {
            return Boolean.FALSE;
        }
        return chain.proceed();
    }
}
final class Prefs extends ContentObserver {
    public static final String KEY = State.setting("system_framework_flag_secure");
    private static boolean registered;
    private final ContentResolver resolver;
    public Prefs(Handler handler, ContentResolver contentResolver) {
        super(handler);
        this.resolver = contentResolver;
    }
    public static synchronized void init(Object obj) {
        if (registered) {
            return;
        }
        try {
            Context context = (Context) R.get(obj, "mSystemContext");
            if (context != null) {
                ContentResolver contentResolver = context.getContentResolver();
                Prefs prefs = new Prefs(new Handler(Looper.getMainLooper()), contentResolver);
                contentResolver.registerContentObserver(Settings.Global.getUriFor(KEY), false, prefs);
                registered = true;
                prefs.refresh();
            }
        } catch (Throwable th) {
            FlagSecure.enabled = false;
            Reflect.log(th);
        }
    }
    @Override // android.database.ContentObserver
    public void onChange(boolean z) {
        refresh();
    }
    @Override // android.database.ContentObserver
    public void onChange(boolean z, Uri uri) {
        refresh();
    }
    public void refresh() {
        try {
            boolean z = Settings.Global.getInt(this.resolver, KEY, 0) == 1;
            FlagSecure.enabled = z;
            Log.i("HyperControl.FlagSecure", z ? "enabled=true; cached Settings.Global preference" : "enabled=false; original behavior");
        } catch (Throwable th) {
            FlagSecure.enabled = false;
            Reflect.log(th);
        }
    }
}
