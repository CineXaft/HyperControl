package com.cinexaft.hypercontrol.flasher;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;

/* JADX INFO: loaded from: classes.dex */
public final class Task implements Runnable {
    public static volatile String digest;
    public static File dir;
    public static volatile int phase;
    public static volatile boolean rebootDismissed;
    public static String scriptDigest;
    private final int action;
    private final Context ctx;
    private final FlashService service;
    private final String uri;
    private static final StringBuilder lines = new StringBuilder();
    private static final StringBuilder rawLog = new StringBuilder();
    public static volatile String info = "";

    public Task(Context context, int i, String str) {
        Context applicationContext = context.getApplicationContext();
        this.ctx = applicationContext != null ? context : applicationContext;
        this.service = null;
        this.action = i;
        this.uri = str;
    }

    public Task(FlashService flashService, int i, String str) {
        this.ctx = flashService;
        this.service = flashService;
        this.action = i;
        this.uri = str;
    }

    public static void cleanupWorkspace(Context context) {
        deleteWorkspaceTree(new File(context.getFilesDir(), "kernel-flasher"));
        dir = null;
        digest = null;
        scriptDigest = null;
    }

    public static synchronized void clearLog() {
        lines.setLength(0);
        rawLog.setLength(0);
    }

    private static void deleteWorkspaceTree(File file) {
        File[] fileArrListFiles;
        if (file != null) {
            if (file.isDirectory() && (fileArrListFiles = file.listFiles()) != null) {
                for (File file2 : fileArrListFiles) {
                    deleteWorkspaceTree(file2);
                }
            }
            file.delete();
        }
    }

    private static void discardCopy(File file) {
        try {
            new File(file, "input.zip").delete();
        } catch (Throwable th) {
        }
    }

    private static void flash() throws IOException, NoSuchAlgorithmException, InterruptedException {
        File file = dir;
        if (file == null || !IO.sha(new File(file, "input.zip")).equals(digest) || !IO.sha(new File(file, "update-binary")).equals(scriptDigest)) {
            throw new SecurityException("Prepared files changed or session was lost. Select the ZIP again.");
        }
        String strQuote = IO.quote(file.getCanonicalPath());
        String command = "[ \"$(id -u)\" = 0 ] || { echo 'Root permission denied'; exit 77; }\ncd " + strQuote + " || exit 78\nlock=../flash.lock\nmkdir \"$lock\" 2>/dev/null || { echo 'Another flash is running or an interrupted flash left a lock; do not retry blindly'; exit 79; }\ntrap 'rmdir \"$lock\"' EXIT\nexport POSTINSTALL=" + strQuote + "\nexport PATH=/system/bin:/system/xbin:$PATH\nsh ./update-binary 3 1 \"$POSTINSTALL/input.zip\"\nrc=$?\nsync\necho \"Updater exit code: $rc\"\nexit \"$rc\"\n";
        log("Root execution starting. Do not force-stop HyperControl or reboot while running.");
        int iRunRoot = runRoot(command);
        if (iRunRoot == 0) {
            log("Updater reported success (exit 0). No automatic reboot.");
        } else {
            throw new IOException("Updater failed, exit=" + iRunRoot);
        }
    }

    public static synchronized void log(String str) {
        StringBuilder sb = lines;
        sb.append(str);
        sb.append("\n");
        int length = sb.length();
        if (length > 32768) {
            sb.delete(0, length - 32768);
        }
        Log.i("HyperControl.Flasher", str);
    }

    public static synchronized void reset() {
        StringBuilder sb = lines;
        rebootDismissed = false;
        sb.setLength(0);
        rawLog.setLength(0);
        File file = dir;
        if (file != null) {
            discardCopy(file);
        }
        digest = null;
        scriptDigest = null;
        dir = null;
        info = "";
    }

    private static int runRoot(String str) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("su", "-c", str);
        processBuilder.redirectErrorStream(true);
        Process processStart = processBuilder.start();
        IO.close(processStart.getOutputStream());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processStart.getInputStream(), "UTF-8"));
        int result;
        try {
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    result = processStart.waitFor();
                    break;
                }
                ui(line);
            }
        } catch (Throwable th) {
            while (true) {
                try {
                    processStart.waitFor();
                    break;
                } catch (InterruptedException e) {
                }
            }
            IO.close(bufferedReader);
            throw th;
        }
        IO.close(bufferedReader);
        return result;
    }

    public static synchronized Bundle snapshot() {
        Bundle bundle = new Bundle();
        bundle.putInt("phase", phase);
        bundle.putBoolean("rebootDismissed", rebootDismissed);
        bundle.putString("info", info);
        bundle.putString("token", digest);
        bundle.putString("log", lines.toString());
        bundle.putString("raw", rawLog.toString());
        return bundle;
    }

    private static synchronized void ui(String str) {
        StringBuilder sb = rawLog;
        sb.append(str);
        sb.append("\n");
        int length = sb.length();
        if (length > 131072) {
            sb.delete(0, length - 131072);
        }
        if (str.startsWith("ui_print")) {
            log(str.replace("ui_print", "").trim());
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        int i;
        try {
            int i2 = this.action;
            if (i2 == 0) {
                Package.prepare(this.ctx, Uri.parse(this.uri));
                log("Package prepared. No partition has been written yet.");
                i = 2;
            } else if (i2 == 1) {
                if (phase != 3) {
                    throw new IllegalStateException("Prepared package is no longer valid; select the ZIP again");
                }
                flash();
                cleanupWorkspace(this.ctx);
                i = 4;
            } else {
                if (runRoot("[ \"$(id -u)\" = 0 ] || exit 77; svc power reboot") != 0) {
                    throw new IOException("Reboot request failed");
                }
                i = 4;
            }
        } catch (Throwable th) {
            log(th.toString());
            Log.e("HyperControl.Flasher", "Task failed; no automatic retry or reboot", th);
            i = 5;
        }
        FlashService flashService = this.service;
        if (flashService != null) {
            flashService.stopForeground(true);
            flashService.stopSelf();
        }
        phase = i;
    }
}
