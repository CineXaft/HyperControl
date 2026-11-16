package dev.local.hypercontrol.flasher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import java.io.File;
import java.io.FileNotFoundException;

/* JADX INFO: loaded from: classes.dex */
public final class KernelProvider extends ContentProvider {
    private static void launch(Context context, int i, String str) {
        Intent intentPutExtra = new Intent(context, (Class<?>) FlashService.class).putExtra("action", i).putExtra("uri", str);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            context.startForegroundService(intentPutExtra);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
            Task.phase = 5;
            throw th;
        }
        Binder.restoreCallingIdentity(jClearCallingIdentity);
    }

    private static void spawn(Context context, int i, String str) {
        Thread thread = new Thread(new Task(context, i, str), "HyperControlKernel");
        try {
            thread.start();
        } catch (Throwable th) {
            Task.phase = 5;
            throw th;
        }
    }

    @Override // android.content.ContentProvider
    public synchronized Bundle call(String str, String str2, Bundle bundle) {
        Context context = getContext();
        context.enforceCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS", "Kernel flasher is restricted to trusted Settings callers");
        boolean invalid = false;
        try {
            if (!"status".equals(str)) {
                if ("discard".equals(str)) {
                    Task.reset();
                    Task.phase = 0;
                    Task.cleanupWorkspace(context);
                } else if ("dismiss_reboot".equals(str)) {
                    Task.rebootDismissed = true;
                } else {
                    int phase = Task.phase;
                    if (phase == 1 || phase == 3) {
                        throw new IllegalStateException("A task is still running");
                    }
                    if ("prepare".equals(str)) {
                        if (str2 != null && "content".equals(Uri.parse(str2).getScheme())) {
                            Task.reset();
                            Task.phase = 1;
                            spawn(context, 0, str2);
                        } else {
                            invalid = true;
                        }
                    } else if ("start".equals(str)) {
                        String digest;
                        if (phase == 2 && (digest = Task.digest) != null && digest.equals(str2)) {
                            Task.clearLog();
                            Task.phase = 3;
                            launch(context, 1, str2);
                        } else {
                            invalid = true;
                        }
                    } else if ("reboot".equals(str)) {
                        if (phase == 4) {
                            Task.phase = 6;
                            spawn(context, 2, null);
                        } else {
                            invalid = true;
                        }
                    }
                }
            }
            if (!invalid) {
                return Task.snapshot();
            }
        } catch (Throwable th) {
            Bundle error = new Bundle();
            error.putString("error", th.toString());
            return error;
        }
        // The shared invalid-session branch is outside the smali catch region.
        throw new IllegalStateException("Prepared package missing or expired; select the ZIP again");
    }

    @Override // android.content.ContentProvider
    public int delete(Uri uri, String str, String[] strArr) {
        return 0;
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
    public ParcelFileDescriptor openFile(Uri uri, String str) throws FileNotFoundException {
        if (Binder.getCallingUid() != Process.myUid()) {
            getContext().enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Kernel flasher is restricted to trusted Settings callers");
        }
        if (!"/inbox".equals(uri.getPath())) {
            throw new IllegalArgumentException("Unknown content path");
        }
        File file = new File(getContext().getFilesDir(), "kernel-flasher");
        file.mkdirs();
        return ParcelFileDescriptor.open(new File(file, "inbox.zip"), "r".equals(str) ? 268435456 : 738197504);
    }

    @Override // android.content.ContentProvider
    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return null;
    }

    @Override // android.content.ContentProvider
    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        return 0;
    }
}
