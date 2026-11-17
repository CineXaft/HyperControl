package com.cinexaft.hypercontrol.ui;

import android.content.Context;
import android.content.res.Resources;

/* JADX INFO: loaded from: classes.dex */
public final class Strings {
    private static final String[] words = "模块|系统框架|系统界面|系统安全组件|电量和性能|HTML 查看器|下载管理|其他|拓展功能|广告服务|保存|取消|重启|重启系统|留空使用默认文本|自定义移动信号文本".split("\\|");
    private static final String[] names = "hc_modules|hc_framework|hc_systemui|hc_guard|hc_power|hc_html|hc_downloads|hc_other|hc_extended|hc_ads|hc_save|hc_cancel|hc_restart|hc_reboot|hc_empty_text|hc_mobile_text".split("\\|");

    private static String extra(Context context, String str) {
        if (str != null) {
            String[] strArrSplit = get(context, "hc_shortcut_source").split("\\|");
            for (int i = 0; i < strArrSplit.length; i++) {
                if (str.equals(strArrSplit[i])) {
                    String[] strArrSplit2 = get(context, "hc_shortcut_titles").split("\\|");
                    if (i >= strArrSplit2.length) {
                        break;
                    }
                    return strArrSplit2[i];
                }
            }
        }
        return str;
    }

    public static String get(Context context, String str) {
        try {
            Resources resources = context.createPackageContext("com.cinexaft.hypercontrol", 0).createConfigurationContext(context.getResources().getConfiguration()).getResources();
            int identifier = resources.getIdentifier(str, "string", "com.cinexaft.hypercontrol");
            return identifier != 0 ? resources.getString(identifier) : str;
        } catch (Throwable th) {
            return str;
        }
    }

    public static String t(Context context, String str) {
        if (str != null) {
            if (str.startsWith("hc_")) {
                return get(context, str);
            }
            String[] strArr = words;
            for (int i = 0; i < strArr.length; i++) {
                if (str.equals(strArr[i])) {
                    String str2 = names[i];
                    String str3 = get(context, str2);
                    if (str2.equals(str3)) {
                        break;
                    }
                    return str3;
                }
            }
        }
        return extra(context, str);
    }
}
