package com.cinexaft.hypercontrol;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Process;
import com.cinexaft.hypercontrol.ui.Strings;

/* JADX INFO: loaded from: classes.dex */
public final class RestartTarget {
    public static String label(Context context, String str) throws PackageManager.NameNotFoundException {
        if ("android".equals(str)) {
            return Strings.get(context, "hc_framework");
        }
        if (str.startsWith("hc.")) {
            if ("hc.other".equals(str)) {
                return Strings.get(context, "hc_other");
            }
            return "hc.power_menu_settings".equals(str) ? Strings.get(context, "hc_extended") : Strings.get(context, "hc_ads");
        }
        if ("com.android.systemui".equals(str)) {
            return Strings.get(context, "hc_systemui");
        }
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getApplicationInfo(str, 0).loadLabel(packageManager).toString();
    }

    public static String script(Context context, String str) throws PackageManager.NameNotFoundException {
        ComponentName component;
        if ("android".equals(str)) {
            return "[ \"$(id -u)\" = 0 ] || exit 20; /system/bin/svc power reboot";
        }
        if (str != null && str.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) {
            if ("com.android.systemui".equals(str)) {
                return "[ \"$(id -u)\" = 0 ] || exit 20; old=$(pidof com.android.systemui); [ -n \"$old\" ] || exit 21; kill -9 $old || exit 22; n=0; while [ $n -lt 12 ]; do sleep 1; new=$(pidof com.android.systemui); if [ -n \"$new\" ] && [ \"$new\" != \"$old\" ]; then exit 0; fi; n=$((n+1)); done; exit 23";
            }
            if ("com.miui.powerkeeper".equals(str)) {
                return "[ \"$(id -u)\" = 0 ] || exit 20; old=$(pidof com.miui.powerkeeper); [ -z \"$old\" ] || kill -9 $old || exit 22; am start-service --user 0 -n com.miui.powerkeeper/com.miui.powerkeeper.PowerKeeperBackgroundService >/dev/null 2>&1; n=0; while [ $n -lt 12 ]; do sleep 1; new=$(pidof com.miui.powerkeeper); if [ -n \"$new\" ] && [ \"$new\" != \"$old\" ]; then exit 0; fi; n=$((n+1)); done; exit 23";
            }
            if ("com.miui.guardprovider".equals(str)) {
                return "[ \"$(id -u)\" = 0 ] || exit 20; old=$(pidof com.miui.guardprovider); extra=$(pidof com.miui.guardprovider:ai); [ -z \"$extra\" ] || kill -9 $extra || exit 22; [ -z \"$old\" ] || kill -9 $old || exit 22; am start-service --user 0 -n com.miui.guardprovider/com.miui.guardprovider.manager.SecurityService >/dev/null 2>&1; n=0; while [ $n -lt 12 ]; do sleep 1; new=$(pidof com.miui.guardprovider); if [ -n \"$new\" ] && [ \"$new\" != \"$old\" ]; then exit 0; fi; n=$((n+1)); done; exit 23";
            }
            if ("com.android.htmlviewer".equals(str)) {
                return "[ \"$(id -u)\" = 0 ] || exit 20; old=$(pidof com.android.htmlviewer:remote); for name in com.android.htmlviewer com.android.htmlviewer:remote com.android.htmlviewer:settings com.android.htmlviewer:pushservice; do p=$(pidof $name); [ -z \"$p\" ] || kill -9 $p || exit 22; done; content gettype --uri content://com.android.settings.cloud.CloudSettings >/dev/null 2>&1; n=0; while [ $n -lt 12 ]; do sleep 1; new=$(pidof com.android.htmlviewer:remote); if [ -n \"$new\" ] && [ \"$new\" != \"$old\" ]; then exit 0; fi; n=$((n+1)); done; exit 23";
            }
            if ("com.miui.securitycenter".equals(str)) {
                return "[ \"$(id -u)\" = 0 ] || exit 20; am force-stop --user " + (Process.myUid() / 100000) + " 'com.miui.securitycenter' || exit 22";
            }
            if ("com.android.providers.downloads".equals(str)) {
                return "[ \"$(id -u)\" = 0 ] || exit 20; old=$(pidof android.process.media); [ -z \"$old\" ] || kill -9 $old || exit 22; content gettype --uri content://downloads/my_downloads >/dev/null 2>&1; n=0; while [ $n -lt 12 ]; do sleep 1; new=$(pidof android.process.media); if [ -n \"$new\" ] && [ \"$new\" != \"$old\" ]; then exit 0; fi; n=$((n+1)); done; exit 23";
            }
            PackageManager packageManager = context.getPackageManager();
            if (packageManager.getApplicationInfo(str, 0).uid % 100000 >= 10000 && !"com.android.settings".equals(str) && !"com.cinexaft.hypercontrol".equals(str)) {
                int iMyUid = Process.myUid() / 100000;
                Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(str);
                if (launchIntentForPackage != null && (component = launchIntentForPackage.getComponent()) != null) {
                    String strFlattenToString = component.flattenToString();
                    if (strFlattenToString.matches("[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+")) {
                        return "[ \"$(id -u)\" = 0 ] || exit 20; am force-stop --user " + iMyUid + " '" + str + "' || exit 22; am start -W --user " + iMyUid + " -n '" + strFlattenToString + "' >/dev/null 2>&1 || exit 23; sleep 1; pidof '" + str + "' >/dev/null || exit 23";
                    }
                }
            }
        }
        throw new IllegalArgumentException("不支持此目标：仅允许系统界面或具有启动入口的非核心应用");
    }
}
