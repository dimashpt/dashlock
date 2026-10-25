package id.sooon.dashlock;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferensi DashLock: aktif/nonaktif, kunci layar, posisi FAB, mode buka kunci. */
final class Prefs {
    private static final String NAME = "dashlock";

    static final int SIDE_LEFT = 0;
    static final int SIDE_RIGHT = 1;

    static final int UNLOCK_HOLD = 0;
    static final int UNLOCK_TRIPLE_TAP = 1;

    private Prefs() { }

    static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    private static void save(SharedPreferences.Editor e) {
        e.commit();
    }

    static boolean isEnabled(Context c) {
        return sp(c).getBoolean("enabled", false);
    }

    static void setEnabled(Context c, boolean v) {
        save(sp(c).edit().putBoolean("enabled", v));
    }

    static boolean isLocked(Context c) {
        return sp(c).getBoolean("locked", false);
    }

    static void setLocked(Context c, boolean v) {
        save(sp(c).edit().putBoolean("locked", v));
    }

    static int fabSide(Context c) {
        return sp(c).getInt("fab_side", SIDE_RIGHT);
    }

    static void setFabSide(Context c, int side) {
        save(sp(c).edit().putInt("fab_side", side));
    }

    static int fabCenterY(Context c) {
        return sp(c).getInt("fab_y", -1);
    }

    static void setFabCenterY(Context c, int yPx) {
        save(sp(c).edit().putInt("fab_y", yPx));
    }

    static boolean hasPin(Context c) {
        return sp(c).contains("pin_hash");
    }

    static int unlockMode(Context c) {
        return sp(c).getInt("unlock_mode", UNLOCK_HOLD);
    }

    static void setUnlockMode(Context c, int mode) {
        save(sp(c).edit().putInt("unlock_mode", mode));
    }

    static int unlockHoldSeconds(Context c) {
        int s = sp(c).getInt("unlock_hold_sec", 2);
        if (s < 1) s = 1;
        if (s > 3) s = 3;
        return s;
    }

    static void setUnlockHoldSeconds(Context c, int seconds) {
        int s = Math.max(1, Math.min(3, seconds));
        save(sp(c).edit().putInt("unlock_hold_sec", s));
    }

    /** Default ON: konfirmasi sebelum mengunci. */
    static boolean lockConfirm(Context c) {
        return sp(c).getBoolean("lock_confirm", true);
    }

    static void setLockConfirm(Context c, boolean v) {
        save(sp(c).edit().putBoolean("lock_confirm", v));
    }

    /** Default OFF: cukup gesture untuk buka kunci. */
    static boolean unlockRequirePin(Context c) {
        return sp(c).getBoolean("unlock_require_pin", false);
    }

    static void setUnlockRequirePin(Context c, boolean v) {
        save(sp(c).edit().putBoolean("unlock_require_pin", v));
    }
}
