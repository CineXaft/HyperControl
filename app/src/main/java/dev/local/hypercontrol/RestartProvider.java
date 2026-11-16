package dev.local.hypercontrol;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/* JADX INFO: loaded from: classes.dex */
public final class RestartProvider extends ContentProvider {
    private static final AtomicBoolean busy = new AtomicBoolean(false);

    private static Bundle reply(int i, String str) {
        Bundle bundle = new Bundle();
        bundle.putInt("code", i);
        bundle.putString("message", str);
        return bundle;
    }

    private static Bundle restart(Context context, String str) {
        Bundle bundleReply;
        Process processStart = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", RestartTarget.script(context, str));
            processBuilder.redirectErrorStream(true);
            processStart = processBuilder.start();
            if (processStart.waitFor(30L, TimeUnit.SECONDS)) {
                int iExitValue = processStart.exitValue();
                if (iExitValue == 0) {
                    bundleReply = reply(0, "android".equals(str) ? "已发送整机重启请求" : "已检测到目标应用进程，请确认界面恢复");
                } else {
                    bundleReply = reply(iExitValue, "重启失败，退出码 " + iExitValue + "（20无root、21无目标、22结束失败、23未恢复；其他为su错误）。请给 HyperControl 授权。");
                }
            } else {
                processStart.destroyForcibly();
                bundleReply = reply(31, "请求超时，请检查 HyperControl 的 root 授权和目标应用状态");
            }
            processStart.getInputStream().close();
            processStart.getOutputStream().close();
            processStart.getErrorStream().close();
            return bundleReply;
        } catch (Throwable th) {
            if (processStart != null) {
                processStart.destroy();
            }
            Log.e("HyperControl", "Root restart failed in module process", th);
            return reply(32, "HyperControl 无法执行重启，请检查 root 授权");
        }
    }

    @Override // android.content.ContentProvider
    public Bundle call(String str, String str2, Bundle bundle) throws PackageManager.NameNotFoundException {
        ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo("com.android.settings", 0);
        if (Binder.getCallingUid() != applicationInfo.uid || (applicationInfo.flags & 1) == 0) {
            throw new SecurityException("Only system Settings UID is allowed");
        }
        if (!"restart_app".equals(str) || str2 == null || (bundle != null && !bundle.isEmpty())) {
            throw new IllegalArgumentException("Only restart_app with validated package name and empty extras is supported");
        }
        if (!busy.compareAndSet(false, true)) {
            return reply(30, "已有重启请求正在执行");
        }
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            return restart(getContext(), str2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            busy.set(false);
        }
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }

    @Override // android.content.ContentProvider
    public String getType(Uri uri) {
        return null;
    }

    @Override // android.content.ContentProvider
    public Uri insert(Uri uri, ContentValues contentValues) {
        throw new UnsupportedOperationException();
    }

    @Override // android.content.ContentProvider
    public boolean onCreate() {
        return true;
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        throw new UnsupportedOperationException();
    }
}
