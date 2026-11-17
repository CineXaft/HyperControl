package com.cinexaft.hypercontrol.flasher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/* JADX INFO: loaded from: classes.dex */
public final class IO {
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Throwable th) {
            }
        }
    }

    public static void copy(InputStream inputStream, File file, long j) throws IOException {
        if (inputStream == null) {
            throw new IOException("Cannot open selected document");
        }
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            byte[] bArr = new byte[65536];
            long j2 = 0;
            while (true) {
                int i = inputStream.read(bArr);
                if (i < 0) {
                    fileOutputStream.getFD().sync();
                    close(fileOutputStream);
                    close(inputStream);
                    return;
                } else {
                    j2 += (long) i;
                    if (j2 > j) {
                        throw new IOException("File exceeds size limit");
                    }
                    fileOutputStream.write(bArr, 0, i);
                }
            }
        } catch (Throwable th) {
            close(fileOutputStream);
            close(inputStream);
            throw th;
        }
    }

    public static String quote(String str) {
        return "'" + str.replace("'", "'\\''") + "'";
    }

    public static String sha(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            byte[] bArr = new byte[65536];
            while (true) {
                int i = fileInputStream.read(bArr);
                if (i < 0) {
                    break;
                }
                messageDigest.update(bArr, 0, i);
            }
            byte[] bArrDigest = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bArrDigest) {
                sb.append(Integer.toHexString((b & 255) + 256).substring(1));
            }
            return sb.toString();
        } finally {
            close(fileInputStream);
        }
    }

    public static String text(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] bArr = new byte[8192];
            do {
                int i = fileInputStream.read(bArr);
                if (i < 0) {
                    String string = byteArrayOutputStream.toString("UTF-8");
                    close(fileInputStream);
                    return string;
                }
                byteArrayOutputStream.write(bArr, 0, i);
            } while (byteArrayOutputStream.size() <= 2097152);
            throw new IOException("Updater script too large");
        } catch (Throwable th) {
            close(fileInputStream);
            throw th;
        }
    }

    public static void write(File file, String str) throws IOException {
        copy(new ByteArrayInputStream(str.getBytes("UTF-8")), file, 4194304L);
    }
}
