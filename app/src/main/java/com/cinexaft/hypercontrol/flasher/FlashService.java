package com.cinexaft.hypercontrol.flasher;

import android.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;

/* JADX INFO: loaded from: classes.dex */
public final class FlashService extends Service {
    private PowerManager.WakeLock wake;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onDestroy() {
        PowerManager.WakeLock wakeLock = this.wake;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent == null) {
            stopSelf();
            return 2;
        }
        try {
            ((NotificationManager) getSystemService("notification")).createNotificationChannel(new NotificationChannel("hc_kernel", "Kernel flasher", 2));
            Notification.Builder builder = new Notification.Builder(this, "hc_kernel");
            builder.setContentTitle("Kernel flasher");
            builder.setContentText("HyperControl: task running; do not force-stop");
            builder.setSmallIcon(R.drawable.ic_dialog_info);
            builder.setOngoing(true);
            startForeground(18507, builder.build(), 1073741824);
            PowerManager.WakeLock wakeLock = this.wake;
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            PowerManager.WakeLock wakeLockNewWakeLock = ((PowerManager) getSystemService("power")).newWakeLock(1, "HyperControl:KernelFlash");
            wakeLockNewWakeLock.setReferenceCounted(false);
            wakeLockNewWakeLock.acquire();
            this.wake = wakeLockNewWakeLock;
            new Thread(new Task(this, intent.getIntExtra("action", 0), intent.getStringExtra("uri")), "HyperControlKernel").start();
            return 2;
        } catch (Throwable th) {
            Task.log(th.toString());
            Task.phase = 5;
            stopForeground(true);
            stopSelf();
            return 2;
        }
    }
}
