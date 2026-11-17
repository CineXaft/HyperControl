package com.cinexaft.hypercontrol.passkey;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public final class Chooser {
    public static ComponentName component(List list) throws IllegalAccessException, NoSuchMethodException, PackageManager.NameNotFoundException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException {
        if (list.size() < 2) {
            return null;
        }
        Object obj = list.get(0);
        if (!(obj instanceof Context)) {
            return null;
        }
        Context context = (Context) obj;
        Object obj2 = list.get(1);
        if (obj2 == null) {
            return null;
        }
        Class<?> cls = obj2.getClass();
        if (!"android.credentials.selection.IntentCreationResult$Builder".equals(cls.getName())) {
            return null;
        }
        ComponentName componentNameUnflattenFromString = ComponentName.unflattenFromString("com.google.android.gms/.identitycredentials.ui.CredentialChooserActivity");
        PackageManager packageManager = context.getPackageManager();
        ActivityInfo activityInfo = packageManager.getActivityInfo(componentNameUnflattenFromString, 1048576);
        if (!activityInfo.exported) {
            return null;
        }
        int componentEnabledSetting = packageManager.getComponentEnabledSetting(componentNameUnflattenFromString);
        if (componentEnabledSetting != 1 && (componentEnabledSetting != 0 || !activityInfo.enabled)) {
            return null;
        }
        Object obj3 = cls.getClassLoader().loadClass("android.credentials.selection.IntentCreationResult$OemUiUsageStatus").getField("SUCCESS").get(null);
        setter(obj2, "setOemUiPackageName", "com.google.android.gms");
        setter(obj2, "setOemUiUsageStatus", obj3);
        return componentNameUnflattenFromString;
    }

    private static void setter(Object obj, String str, Object obj2) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        for (Method method : obj.getClass().getMethods()) {
            if (str.equals(method.getName())) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].isInstance(obj2)) {
                    method.invoke(obj, obj2);
                    return;
                }
            }
        }
        throw new NoSuchMethodException(str);
    }
}
