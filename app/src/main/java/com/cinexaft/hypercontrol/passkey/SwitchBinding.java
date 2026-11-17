package com.cinexaft.hypercontrol.passkey;

import android.view.View;
import android.widget.CompoundButton;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* JADX INFO: loaded from: classes.dex */
public final class SwitchBinding implements View.OnClickListener {
    private final CompoundButton button;
    private final Object preference;

    public SwitchBinding(Object obj, CompoundButton compoundButton) {
        this.preference = obj;
        this.button = compoundButton;
    }

    public static void bind(Object obj, Object obj2) throws IllegalAccessException, NoSuchFieldException, NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        View view;
        Class<?> cls = obj.getClass();
        Field fieldFind = Fields.find(cls, "mSwitch");
        if (fieldFind.get(obj) != null || (view = (View) Fields.find(obj2.getClass(), "itemView").get(obj2)) == null) {
            return;
        }
        View viewFindViewById = view.findViewById(Fields.find(cls.getClassLoader().loadClass("com.android.settingslib.R$id"), "switchWidget").getInt(null));
        if (viewFindViewById instanceof CompoundButton) {
            CompoundButton compoundButton = (CompoundButton) viewFindViewById;
            compoundButton.setChecked(Fields.find(cls, "mChecked").getBoolean(obj));
            compoundButton.setOnClickListener(new SwitchBinding(obj, compoundButton));
            Fields.write(fieldFind, obj, compoundButton);
            describe(obj);
        }
    }

    private static Method callback(Class cls, Class cls2) throws NoSuchMethodException {
        Method method = null;
        for (Method method2 : cls.getMethods()) {
            Class<?>[] parameterTypes = method2.getParameterTypes();
            if (parameterTypes.length == 2 && parameterTypes[0].isAssignableFrom(cls2)) {
                Class<?> cls3 = parameterTypes[1];
                Class<?> cls4 = Boolean.TYPE;
                if (cls3 == cls4 && method2.getReturnType() == cls4) {
                    if ("onCheckChanged".equals(method2.getName())) {
                        return method2;
                    }
                    if (method != null) {
                        throw new NoSuchMethodException("No unique credential switch callback");
                    }
                    method = method2;
                }
            }
        }
        if (method != null) {
            return method;
        }
        throw new NoSuchMethodException("No unique credential switch callback");
    }

    private static void describe(Object obj) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Method declaredMethod = obj.getClass().getDeclaredMethod("maybeUpdateContentDescription", new Class[0]);
        declaredMethod.setAccessible(true);
        declaredMethod.invoke(obj, new Object[0]);
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        Object obj;
        try {
            Object obj2 = this.preference;
            CompoundButton compoundButton = this.button;
            Class<?> cls = obj2.getClass();
            Field fieldFind = Fields.find(cls, "mChecked");
            if (!Policy.enabled() || (obj = Fields.find(cls, "mOnClickListener").get(obj2)) == null) {
                compoundButton.setChecked(fieldFind.getBoolean(obj2));
            } else {
                Method methodCallback = callback(obj.getClass(), cls);
                methodCallback.setAccessible(true);
                if (!Boolean.TRUE.equals(methodCallback.invoke(obj, obj2, Boolean.valueOf(compoundButton.isChecked())))) {
                    Fields.write(fieldFind, obj2, Boolean.FALSE);
                    compoundButton.setChecked(fieldFind.getBoolean(obj2));
                }
            }
            describe(obj2);
        } catch (Throwable th) {
            Policy.warn("Credential switch listener failed", th);
        }
    }
}
