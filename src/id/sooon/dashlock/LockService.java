package id.sooon.dashlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class LockService extends Service {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.wrap(base));
    }

    static final String ACTION_STOP = "id.sooon.dashlock.STOP";
    static final String ACTION_SYNC = "id.sooon.dashlock.SYNC";
    private static final int NOTIF_ID = 1;

    private OverlayController overlay;

    public static void start(Context c) {
        Intent i = new Intent(c, LockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            c.startForegroundService(i);
        } else {
            c.startService(i);
        }
    }

    public static void stop(Context c) {
        Intent i = new Intent(c, LockService.class);
        i.setAction(ACTION_STOP);
        c.startService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Prefs.setLocked(this, false);
            tearDown();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        ensureChannel();
        startForeground(NOTIF_ID, buildNotification());
        boolean sync = intent != null && ACTION_SYNC.equals(intent.getAction());
        if (sync && overlay != null) {
            overlay.detach();
            overlay = null;
        }
        if (overlay == null) overlay = new OverlayController(this);
        overlay.attach();
        overlay.refreshState();
        if (sync) updateNotification();
        return START_STICKY;
    }

    void updateNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification());
    }

    private void tearDown() {
        if (overlay != null) {
            overlay.detach();
            overlay = null;
        }
    }

    @Override
    public void onDestroy() {
        tearDown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel ch = new NotificationChannel(
                "dashlock", getString(R.string.notification_channel), NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String body = Prefs.isLocked(this)
                ? getString(R.string.notification_locked)
                : getString(R.string.notification_unlocked);
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, "dashlock")
                : new Notification.Builder(this);
        return b.setContentTitle(getString(R.string.notification_title))
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }
}
