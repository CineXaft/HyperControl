package com.cinexaft.hypercontrol.flasher;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/* JADX INFO: loaded from: classes.dex */
public final class Package {
    public static void prepare(Context context, Uri uri) throws NoSuchAlgorithmException, IOException {
        if (!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a")) {
            throw new IOException("Horizon mkbootfs helper requires arm64-v8a");
        }
        File file = new File(context.getFilesDir(), "kernel-flasher");
        file.mkdirs();
        File fileCreateTempFile = File.createTempFile("session-", ".work", file);
        fileCreateTempFile.delete();
        if (!fileCreateTempFile.mkdir()) {
            throw new IOException("Cannot create private workspace");
        }
        Task.dir = fileCreateTempFile;
        File file2 = new File(fileCreateTempFile, "input.zip");
        Task.log("Copying selected document to private workspace...");
        IO.copy(context.getContentResolver().openInputStream(uri), file2, 1073741824L);
        String strSha = IO.sha(file2);
        Task.digest = strSha;
        ZipFile zipFile = new ZipFile(file2);
        File file3;
        try {
            ZipEntry entry = zipFile.getEntry("META-INF/com/google/android/update-binary");
            if (entry == null || zipFile.getEntry("anykernel.sh") == null) {
                throw new IOException("Not a supported AnyKernel ZIP, duplicate updater or unsafe ZIP paths");
            }
            Enumeration<? extends ZipEntry> enumerationEntries = zipFile.entries();
            int count = 0;
            int updaterCount = 0;
            while (enumerationEntries.hasMoreElements()) {
                if (++count > 20000) {
                    throw new IOException("Not a supported AnyKernel ZIP, duplicate updater or unsafe ZIP paths");
                }
                String name = enumerationEntries.nextElement().getName();
                if (name.startsWith("/") || name.contains("\\") || name.contains("/../")
                        || name.startsWith("../") || name.endsWith("/..")
                        || ("META-INF/com/google/android/update-binary".equals(name) && ++updaterCount > 1)) {
                    throw new IOException("Not a supported AnyKernel ZIP, duplicate updater or unsafe ZIP paths");
                }
            }
            file3 = new File(fileCreateTempFile, "update-binary");
            IO.copy(zipFile.getInputStream(entry), file3, 2097152L);
        } catch (Throwable th) {
            IO.close(zipFile);
            throw th;
        }
        IO.close(zipFile);
        String strText = IO.text(file3);
        if (!strText.startsWith("#!") || strText.indexOf(0) >= 0) {
            throw new IOException("Only shell update-binary scripts are supported, not native recovery binaries");
        }
        File file4 = new File(fileCreateTempFile, "mkbootfs");
        IO.copy(context.getAssets().open("mkbootfs"), file4, 1048576L);
        file4.setExecutable(true, false);
        if (strText.contains("$BB chmod -R 755 tools bin;")) {
            IO.write(file3, strText.replace("$BB chmod -R 755 tools bin;", "cp -f " + IO.quote(file4.getAbsolutePath()) + " \"$AKHOME/tools/mkbootfs\" || exit 1\n$BB chmod -R 755 tools bin;"));
            Task.log("Horizon mkbootfs compatibility patch applied.");
        } else {
            Task.log("No legacy mkbootfs marker; updater kept unchanged.");
        }
        Task.scriptDigest = IO.sha(file3);
        Task.info = "AnyKernel ZIP\nSource: " + uri.getLastPathSegment() + "\nBytes: " + file2.length() + "\nSHA-256: " + strSha + "\nDevice: " + Build.DEVICE + "\nCompatibility and backup are controlled by the selected package.";
    }
}
