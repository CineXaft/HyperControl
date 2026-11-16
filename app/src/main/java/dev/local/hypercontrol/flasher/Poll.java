package dev.local.hypercontrol.flasher;

import android.os.Bundle;

/* JADX INFO: loaded from: classes.dex */
public final class Poll implements Runnable {
    private Bundle data;
    private final Page page;
    private final boolean pushOnly;
    private boolean ui;

    public Poll(Page page) {
        this.page = page;
        this.pushOnly = false;
    }

    public Poll(Page page, Bundle bundle) {
        this.page = page;
        this.data = bundle;
        this.ui = true;
        this.pushOnly = true;
    }

    @Override // java.lang.Runnable
    public void run() {
        if (!this.ui) {
            this.data = this.page.fetch();
            this.ui = true;
            this.page.ui(this);
        } else {
            try {
                this.page.apply(this.data);
            } catch (Throwable th) {
            }
            if (this.pushOnly) {
                return;
            }
            this.page.done();
        }
    }
}
