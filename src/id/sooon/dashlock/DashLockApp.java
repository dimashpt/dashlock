package id.sooon.dashlock;

import android.app.Application;

/** Mulai ulang layanan overlay bila pengaturan masih aktif (setelah proses app hidup lagi). */
public class DashLockApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        StartReceiver.tryStartService(this);
    }
}
