package dev.local.hypercontrol.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import dev.local.hypercontrol.RestartTarget;
import java.util.concurrent.atomic.AtomicBoolean;
import miuix.appcompat.app.AlertDialog;

/* JADX INFO: loaded from: classes.dex */
public final class Restart implements Runnable, DialogInterface.OnClickListener {
    private static AtomicBoolean busy = new AtomicBoolean(false);
    private Activity activity;
    private boolean launchSecurityCenter;
    private String message;
    private String target;
    private boolean ui;

    private Restart(Activity activity) {
        this.activity = activity;
    }

    public static void confirm(Activity activity) {
        confirm(activity, "com.android.systemui");
    }

    public static void confirm(Activity activity, String str) {
        Restart restart = new Restart(activity);
        restart.target = str;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        RestartTarget.label(activity, str);
        String strConcat = Strings.get(activity, "hc_restart").concat(" ").concat(RestartTarget.label(activity, str));
        if ("android".equals(str)) {
            strConcat = Strings.get(activity, "hc_rebootquestion");
        }
        builder.setTitle(strConcat);
        String str2 = Strings.get(activity, "hc_confirmapp");
        if ("com.android.providers.downloads".equals(str)) {
            str2 = Strings.get(activity, "hc_confirmdownload");
        }
        if ("android".equals(str)) {
            str2 = Strings.get(activity, "hc_confirmsystem");
        }
        builder.setMessage(str2);
        builder.setPositiveButton(Strings.get(activity, "hc_restart"), restart);
        builder.setNegativeButton(Strings.get(activity, "hc_cancel"), (DialogInterface.OnClickListener) null);
        builder.show();
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1 && busy.compareAndSet(false, true)) {
            new Thread(this, "HyperControlRestartUI").start();
        }
    }

    /* JADX WARN: Code duplicated, block: B:14:0x003c A[Catch: Throwable -> 0x0041, TRY_LEAVE, TryCatch #0 {Throwable -> 0x0041, blocks: (B:4:0x0004, B:6:0x001b, B:8:0x0024, B:10:0x002e, B:11:0x0031, B:13:0x0039, B:14:0x003c), top: B:32:0x0004 }] */
    @Override // java.lang.Runnable
    public void run() {
        if (this.ui) {
            Activity activity = this.activity;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
            if (this.launchSecurityCenter) {
                try {
                    activity.startActivity(new Intent().setComponent(new ComponentName("com.miui.securitycenter", "com.miui.securityscan.MainActivity")));
                    this.message = "安全服务已重新打开";
                } catch (Throwable th) {
                    Log.e("HyperControl", "Security Center launch from Settings failed", th);
                    this.message = "安全服务已停止，但启动界面失败";
                }
            }
            Toast.makeText(activity, this.message, 1).show();
            return;
        }
        try {
            Bundle bundleCall = this.activity.getContentResolver().call(Uri.parse("content://com.cinexaft.hypercontrol.restart"), "restart_app", this.target, (Bundle) null);
            if (bundleCall == null) {
                this.message = "HyperControl 重启接口未返回结果";
            } else {
                if (bundleCall.getInt("code", -1) == 0 && "com.miui.securitycenter".equals(this.target)) {
                    this.launchSecurityCenter = true;
                }
                String string = bundleCall.getString("message");
                if (string != null) {
                    this.message = string;
                } else {
                    this.message = "HyperControl 重启接口未返回结果";
                }
            }
        } catch (Throwable th2) {
            Log.e("HyperControl", "Restart provider call failed", th2);
            this.message = "无法连接 HyperControl 重启接口：" + th2.toString();
        }
        busy.set(false);
        this.ui = true;
        this.activity.runOnUiThread(this);
    }
}
