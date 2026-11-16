package dev.local.hypercontrol.ui;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/* JADX INFO: loaded from: classes.dex */
public final class PageBridge {
    public static void show(Activity activity, Bundle bundle, CharSequence charSequence) {
        FragmentActivity fragmentActivity = (FragmentActivity) activity;
        FragmentManager supportFragmentManager = fragmentActivity.getSupportFragmentManager();
        if (supportFragmentManager.findFragmentByTag("hypercontrol_native_page") != null) {
            return;
        }
        Bundle bundle2 = bundle == null ? new Bundle() : new Bundle(bundle);
        if (!bundle2.containsKey("title")) {
            if (charSequence == null) {
                charSequence = "HyperControl";
            }
            bundle2.putString("title", charSequence.toString());
        }
        NativeScreen nativeScreen = new NativeScreen();
        nativeScreen.setArguments(bundle2);
        int identifier = fragmentActivity.getResources().getIdentifier("main_content", "id", "com.android.settings");
        if (identifier == 0) {
            throw new IllegalStateException("Settings main_content container not found");
        }
        FragmentTransaction fragmentTransactionBeginTransaction = supportFragmentManager.beginTransaction();
        fragmentTransactionBeginTransaction.replace(identifier, nativeScreen, "hypercontrol_native_page");
        fragmentTransactionBeginTransaction.commitAllowingStateLoss();
        supportFragmentManager.executePendingTransactions();
    }
}
