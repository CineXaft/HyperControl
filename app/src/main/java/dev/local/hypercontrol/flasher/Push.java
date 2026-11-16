package dev.local.hypercontrol.flasher;

/* JADX INFO: loaded from: classes.dex */
public final class Push implements Runnable {
    private final String arg;
    private final String method;
    private final Page page;

    public Push(Page page, String str, String str2) {
        this.page = page;
        this.method = str;
        this.arg = str2;
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            this.page.submit(this.method, this.arg);
        } catch (Throwable th) {
            this.page.failTo(th);
        }
    }
}
