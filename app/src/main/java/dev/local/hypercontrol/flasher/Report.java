package dev.local.hypercontrol.flasher;

/* JADX INFO: loaded from: classes.dex */
public final class Report implements Runnable {
    private final Throwable error;
    private final Page page;

    public Report(Page page, Throwable th) {
        this.page = page;
        this.error = th;
    }

    @Override // java.lang.Runnable
    public void run() {
        this.page.failDirect(this.error);
    }
}
