package id.sooon.dashlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Menjalankan DashLock setelah boot, user unlock, atau pemasangan ulang APK.
 */
public class StartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_USER_UNLOCKED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            tryStartService(context);
        }
    }

    static void tryStartService(Context context) {
        Context app = context.getApplicationContext();
        if (Prefs.isEnabled(app) && Perm.overlay(app)) {
            LockService.start(app);
        }
    }
}
