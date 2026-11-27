package com.cinexaft.hypercontrol.base;

import android.content.ContentProvider; import android.content.ContentValues; import android.content.Context; import android.content.pm.ApplicationInfo; import android.content.pm.PackageManager; import android.database.Cursor;
import android.net.Uri; import android.os.Binder; import android.os.Bundle; import android.os.Process; import android.util.Log; import com.cinexaft.hypercontrol.R;
import com.cinexaft.hypercontrol.base.Entry.Targets; import java.io.File; import java.io.IOException; import java.util.concurrent.TimeUnit; import java.util.concurrent.atomic.AtomicBoolean;

public final class RestartProvider extends ContentProvider {
    static final String METHOD = "restart_app";
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);

    private static Bundle reply(int code, String message) {
        Bundle bundle = new Bundle();
        bundle.putInt("code", code);
        bundle.putString("message", message);
        return bundle;
    }
    private static Bundle restart(Context context, String target) {
        java.lang.Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command(context, target))
                    .redirectErrorStream(true).redirectOutput(new File("/dev/null")).start();
            if (!process.waitFor(10, TimeUnit.SECONDS))
                return reply(31, context.getString(R.string.hc_restart_timeout));
            int code = process.exitValue();
            int message = Targets.FRAMEWORK.equals(target) || Targets.SYSTEM_UI.equals(target)
                    ? R.string.hc_restart_sent : R.string.hc_stopped;
            return code == 0 ? reply(0, context.getString(message))
                    : reply(code, context.getString(R.string.hc_restart_failed, code));
        } catch (PackageManager.NameNotFoundException error) {
            return reply(33, context.getString(R.string.hc_unavailable));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return reply(32, context.getString(R.string.hc_restart_timeout));
        } catch (IOException | RuntimeException error) {
            Log.e("HyperControl", "Root operation failed", error);
            return reply(32, context.getString(R.string.hc_restart_error));
        } finally {
            if (process != null) {
                // Only the direct su process is controllable; backend cancellation is not guaranteed.
                if (process.isAlive()) {
                    process.destroyForcibly();
                    try {
                        if (!process.waitFor(1, TimeUnit.SECONDS))
                            Log.w("HyperControl", "su termination unconfirmed; operation outcome unknown");
                    } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                }
                try { process.getOutputStream().close(); } catch (IOException ignored) { }
            }
        }
    }
    // Targets are validated before command construction; never interpolate caller-supplied shell text.
    private static String command(Context context, String target) throws PackageManager.NameNotFoundException {
        String root = "[ \"$(id -u)\" = 0 ] || exit 20; ";
        if (Targets.FRAMEWORK.equals(target)) return root + "exec reboot";
        ApplicationInfo app = context.getPackageManager().getApplicationInfo(target, 0);
        String stop = "am force-stop --user " + (Process.myUid() / 100000) + " '" + target + "'";
        if (!Targets.SYSTEM_UI.equals(target)) return root + "exec " + stop;
        return root + "pids=; for pid in $(pidof " + Targets.SYSTEM_UI + "); do "
                + "while read key uid rest; do if [ \"$key\" = Uid: ] && [ \"$uid\" = " + app.uid
                + " ]; then pids=\"$pids $pid\"; break; fi; done < /proc/$pid/status; done; "
                + "if [ -n \"$pids\" ] && kill -9 $pids; then exit 0; fi; exec " + stop;
    }
    @Override
    public Bundle call(String method, String target, Bundle extras) {
        Context context = getContext();
        ApplicationInfo settings;
        try {
            settings = context.getPackageManager().getApplicationInfo(Targets.SETTINGS, 0);
        } catch (PackageManager.NameNotFoundException error) {
            throw new SecurityException("System Settings package not found", error);
        }
        if (Binder.getCallingUid() != settings.uid || (settings.flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            throw new SecurityException("Only system Settings UID is allowed");
        if (!METHOD.equals(method) || !Targets.canRestart(target) || (extras != null && !extras.isEmpty()))
            throw new IllegalArgumentException("Invalid restart request");
        if (!BUSY.compareAndSet(false, true)) return reply(30, context.getString(R.string.hc_busy));
        long identity = Binder.clearCallingIdentity();
        try { return restart(context, target); }
        finally {
            Binder.restoreCallingIdentity(identity);
            BUSY.set(false);
        }
    }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public boolean onCreate() { return true; }
    @Override public Cursor query(Uri uri, String[] columns, String selection, String[] args, String order) { return null; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
