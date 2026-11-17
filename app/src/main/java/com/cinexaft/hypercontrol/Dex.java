package com.cinexaft.hypercontrol;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.luckypray.dexkit.DexKitBridge;

/**
 * Opens the DEX index of the host process.
 *
 * <p>DexKit is a native parser, so its engine lives in {@code libdexkit.so}. That file sits in
 * this module's APK, which is not on the host's library path, so it is unpacked once into the
 * host's code cache and loaded by absolute path. Building the index costs a few dozen
 * milliseconds, which is why the bridge is kept for the lifetime of the process.
 *
 * <p>Everything here fails open: a missing library, an unpacked module or a class loader DexKit
 * cannot read leaves the caller with {@code null} instead of an exception, and the feature that
 * asked for the index simply stays off.
 */
public final class Dex {

    /** Only ABI the module ships; the APK is arm64 only, like the ROM it targets. */
    private static final String LIB = "lib/arm64-v8a/libdexkit.so";

    private static DexKitBridge bridge;
    private static boolean failed;

    private Dex() {
    }

    /**
     * Returns the index for {@code loader}, or {@code null} when it cannot be built.
     *
     * <p>{@code loader} must be the host's own class loader: that is what makes DexKit see the
     * application's dex files, including the split APKs MIUI loads for system modules.
     */
    public static synchronized DexKitBridge of(Context context, ClassLoader loader) {
        if (bridge != null || failed) {
            return bridge;
        }
        try {
            load(context);
            // false: this module runs a handful of fixed queries per process, so the full
            // in-memory cache would cost more than it saves.
            bridge = DexKitBridge.create(loader, false);
            Log.i(Hooks.TAG, "dex index ready, " + bridge.getDexNum() + " dex");
        } catch (Throwable error) {
            failed = true;
            Reflect.log(error);
        }
        return bridge;
    }

    /** Unpacks {@code libdexkit.so} when needed and loads it into this process. */
    private static void load(Context context) throws Exception {
        File out = new File(directory(context), "libdexkit.so");
        try (ZipFile zip = new ZipFile(apk(context))) {
            ZipEntry entry = zip.getEntry(LIB);
            if (entry == null) {
                throw new IllegalStateException("module apk has no " + LIB);
            }
            if (!out.isFile() || out.length() != entry.getSize()) {
                // Write beside the target and rename, so a process killed mid-copy never leaves a
                // truncated library that the next start would load.
                File tmp = new File(directory(context), "libdexkit.tmp");
                copy(zip.getInputStream(entry), tmp);
                if (!tmp.renameTo(out)) {
                    throw new IllegalStateException("cannot install " + out);
                }
            }
        }
        System.load(out.getAbsolutePath());
    }

    private static File directory(Context context) {
        File dir = context.getCodeCacheDir();
        if (dir == null) {
            dir = context.getCacheDir();
        }
        return dir;
    }

    /** This module's own APK; the framework knows it even when the host cannot query it. */
    private static String apk(Context context) throws Exception {
        ApplicationInfo info;
        try {
            info = Entry.module.getModuleApplicationInfo();
        } catch (Throwable error) {
            info = null;
        }
        if (info != null && info.sourceDir != null) {
            return info.sourceDir;
        }
        return context.getPackageManager()
                .getApplicationInfo("com.cinexaft.hypercontrol", 0).sourceDir;
    }

    private static void copy(InputStream input, File target) throws Exception {
        try (OutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
        } finally {
            input.close();
        }
    }
}
