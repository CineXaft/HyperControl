package com.cinexaft.hypercontrol.ui;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.preference.Preference;

/* JADX INFO: loaded from: classes.dex */
public final class Compat {
    public static boolean available(Context context, String str) {
        if (str.startsWith("hc.")) {
            return true;
        }
        try {
            context.getPackageManager().getApplicationInfo(str, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void icon(Preference preference, Context context, String str) {
        Drawable applicationIcon;
        try {
            Resources resources = context.getResources();
            String packageName = context.getPackageName();
            int identifier = resources.getIdentifier("header_icon_size", "dimen", packageName);
            int dimensionPixelSize = identifier != 0 ? resources.getDimensionPixelSize(identifier) : (int) (resources.getDisplayMetrics().density * 28.0f);
            PackageManager packageManager = context.getPackageManager();
            if ("hc.other".equals(str)) {
                int identifier2 = resources.getIdentifier("ic_other_advanced_settings", "drawable", packageName);
                applicationIcon = (identifier2 == 0 && (identifier2 = resources.getIdentifier("ic_more_applications_settings", "drawable", packageName)) == 0) ? packageManager.getDefaultActivityIcon() : context.getDrawable(identifier2);
            } else {
                applicationIcon = packageManager.getApplicationIcon(str);
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(dimensionPixelSize, dimensionPixelSize, Bitmap.Config.ARGB_8888);
            bitmapCreateBitmap.setDensity(resources.getDisplayMetrics().densityDpi);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            applicationIcon.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize);
            applicationIcon.draw(canvas);
            preference.setIcon(new BitmapDrawable(resources, bitmapCreateBitmap));
            int identifier3 = resources.getIdentifier("miuix_preference_settings_main_layout", "layout", packageName);
            if (identifier3 != 0) {
                preference.setLayoutResource(identifier3);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
