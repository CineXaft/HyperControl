package dev.local.hypercontrol;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import dalvik.system.PathClassLoader;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Hook implements XposedInterface.Hooker {
    private static Method bridge;
    public static ClassLoader host;
    private static Class screen;
    private final int mode;

    public Hook(int i) {
        this.mode = i;
    }

    /* JADX WARN: Type inference failed for: r0v5, types: [java.lang.CharSequence, java.lang.Object] */
    /* JADX WARN: Type inference failed for: r1v9, types: [java.lang.CharSequence, java.lang.Object] */
    private static void append(Context context, List list) throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        Object obj;
        Iterator it = list.iterator();
        while (it.hasNext()) {
            if (TextUtils.equals("hypercontrol_entry", Reflect.getObjectField(it.next(), "key"))) {
                return;
            }
        }
        Resources resources = context.getResources();
        int identifier = resources.getIdentifier("my_device", "id", "com.android.settings");
        int i = 0;
        while (true) {
            if (i >= list.size()) {
                Log.w("HyperControl.Entry", "My device header not found; skip insertion rather than use the wrong location");
                return;
            }
            obj = list.get(i);
            if (!TextUtils.equals("hypercontrol_entry", Reflect.getObjectField(obj, "key"))) {
                if ((identifier != 0 && ((int) Reflect.field(obj, "id").getLong(obj)) == identifier) || "com.android.settings.device.MiuiMyDeviceSettings".equals(Reflect.getObjectField(obj, "fragment"))) {
                    break;
                } else {
                    i++;
                }
            } else {
                return;
            }
        }
        int identifier2 = resources.getIdentifier("dynamic_item_icon", "drawable", "com.android.settings");
        if (identifier2 == 0) {
            Field declaredField = host.loadClass("com.android.settings.R$drawable").getDeclaredField("dynamic_item_icon");
            declaredField.setAccessible(true);
            identifier2 = declaredField.getInt(null);
            if (identifier2 == 0) {
                Log.w("HyperControl.Entry", "dynamic_item_icon not found; no development-icon fallback");
                return;
            }
        }
        Object objNewInstance = obj.getClass().newInstance();
        Reflect.setIntField(objNewInstance, "groupId", Reflect.field(obj, "groupId").getInt(obj));
        Reflect.setIntField(objNewInstance, "iconRes", identifier2);
        Reflect.setObjectField(objNewInstance, "key", "hypercontrol_entry");
        Reflect.setObjectField(objNewInstance, "title", "HyperControl");
        Reflect.setObjectField(objNewInstance, "breadCrumbTitle", "HyperControl");
        Reflect.setObjectField(objNewInstance, "fragment", "dev.local.hypercontrol.ui.NativeScreen");
        Bundle bundle = new Bundle();
        bundle.putString("title", "HyperControl");
        Reflect.setObjectField(objNewInstance, "fragmentArguments", bundle);
        Reflect.setObjectField(objNewInstance, "extras", new Bundle());
        Reflect.setLongField(objNewInstance, "id", 1213809409L);
        Entry.apk = context.createPackageContext("com.cinexaft.hypercontrol", 0).getApplicationInfo().sourceDir;
        list.add(i + 1, objNewInstance);
    }

    private static synchronized Class loadScreen() throws ClassNotFoundException {
        Class cls = screen;
        if (cls != null) {
            return cls;
        }
        String str = Entry.apk;
        if (str == null) {
            str = ActivityThread.currentApplication().getPackageManager().getApplicationInfo("com.cinexaft.hypercontrol", 0).sourceDir;
            Entry.apk = str;
        }
        PathClassLoader pathClassLoader = new PathClassLoader(str, host);
        bridge = pathClassLoader.loadClass("dev.local.hypercontrol.ui.PageBridge").getMethod("show", Activity.class, Bundle.class, CharSequence.class);
        Class<?> clsLoadClass = pathClassLoader.loadClass("dev.local.hypercontrol.ui.NativeScreen");
        screen = clsLoadClass;
        return clsLoadClass;
    }

    /* JADX WARN: Code duplicated, block: B:13:0x0027  */
    /* JADX WARN: Code duplicated, block: B:15:0x002e A[RETURN] */
    /* JADX WARN: Code duplicated, block: B:16:0x002f  */
    public Object intercept(XposedInterface.Chain chain) throws IllegalAccessException, ClassNotFoundException, InvocationTargetException {
        Class clsLoadScreen;
        int i = this.mode;
        if (i == 0) {
            Object objProceed = chain.proceed();
            try {
                append((Context) chain.getThisObject(), (List) chain.getArgs().get(0));
                return objProceed;
            } catch (Throwable th) {
                Reflect.log(th);
                return objProceed;
            }
        }
        List args = chain.getArgs();
        int i2 = 1;
        if (i == 1) {
            if (args.size() == 5) {
                i2 = 0;
                if ("dev.local.hypercontrol.ui.NativeScreen".equals(args.get(i2))) {
                    clsLoadScreen = loadScreen();
                    if (i != 1) {
                        return clsLoadScreen;
                    }
                    bridge.invoke(null, chain.getThisObject(), args.get(1), args.get(4));
                    return null;
                }
            }
        } else if (args.size() == 2) {
            if ("dev.local.hypercontrol.ui.NativeScreen".equals(args.get(i2))) {
                clsLoadScreen = loadScreen();
                if (i != 1) {
                    return clsLoadScreen;
                }
                bridge.invoke(null, chain.getThisObject(), args.get(1), args.get(4));
                return null;
            }
        }
        return chain.proceed();
    }
}
