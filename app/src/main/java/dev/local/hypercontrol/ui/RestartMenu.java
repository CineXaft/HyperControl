package dev.local.hypercontrol.ui;

import android.app.Activity;
import android.view.MenuItem;

/* JADX INFO: loaded from: classes.dex */
public final class RestartMenu implements MenuItem.OnMenuItemClickListener {
    private final Activity activity;
    private final String target;

    public RestartMenu(Activity activity, String str) {
        this.activity = activity;
        this.target = str;
    }

    @Override // android.view.MenuItem.OnMenuItemClickListener
    public boolean onMenuItemClick(MenuItem menuItem) {
        Activity activity = this.activity;
        if (activity == null) {
            return true;
        }
        Restart.confirm(activity, this.target);
        return true;
    }
}
