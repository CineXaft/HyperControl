package com.cinexaft.hypercontrol.flasher;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import java.io.InputStream;
import java.io.OutputStream;

/* JADX INFO: loaded from: classes.dex */
public final class Copy implements Runnable {
    private static final String INBOX = "content://com.cinexaft.hypercontrol.flasher/inbox";
    private final Context ctx;
    private final Page page;
    private final Uri uri;

    public Copy(Page page, Context context, Uri uri) {
        this.page = page;
        this.ctx = context;
        this.uri = uri;
    }

    @Override // java.lang.Runnable
    public void run() {
        InputStream inputStreamOpenInputStream = null;
        OutputStream outputStreamOpenOutputStream = null;
        try {
            ContentResolver contentResolver = this.ctx.getContentResolver();
            inputStreamOpenInputStream = contentResolver.openInputStream(this.uri);
            outputStreamOpenOutputStream = contentResolver.openOutputStream(Uri.parse(INBOX));
            byte[] bArr = new byte[16384];
            while (true) {
                int i = inputStreamOpenInputStream.read(bArr);
                if (i < 0) {
                    outputStreamOpenOutputStream.flush();
                    IO.close(outputStreamOpenOutputStream);
                    IO.close(inputStreamOpenInputStream);
                    this.page.submit("prepare", INBOX);
                    return;
                }
                outputStreamOpenOutputStream.write(bArr, 0, i);
            }
        } catch (Throwable th) {
            IO.close(outputStreamOpenOutputStream);
            IO.close(inputStreamOpenInputStream);
            this.page.failTo(th);
        }
    }
}
