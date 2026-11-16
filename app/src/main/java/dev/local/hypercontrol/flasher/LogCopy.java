package dev.local.hypercontrol.flasher;

import android.content.DialogInterface;

/* JADX INFO: loaded from: classes.dex */
public final class LogCopy implements DialogInterface.OnClickListener {
    private final Page page;

    public LogCopy(Page page) {
        this.page = page;
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        this.page.copy();
    }
}
