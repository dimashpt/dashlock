package id.sooon.dashlock;

import android.app.Application;
import android.content.Context;

/** Mulai ulang layanan overlay bila pengaturan masih aktif (setelah proses app hidup lagi). */
public class DashLockApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StartReceiver.tryStartService(this);
    }
}
