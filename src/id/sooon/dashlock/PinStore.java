package id.sooon.dashlock;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Hash PIN (SHA-256 + salt); tidak menyimpan PIN sebagai teks biasa. */
final class PinStore {
    private PinStore() { }

    static boolean setPin(Context c, String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 6 || !pin.matches("\\d+")) return false;
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String hash = hash(pin, salt);
        Prefs.sp(c).edit()
                .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("pin_hash", hash)
                .commit();
        return true;
    }

    static boolean verify(Context c, String pin) {
        if (pin == null || !Prefs.hasPin(c)) return false;
        SharedPreferences sp = Prefs.sp(c);
        String saltB64 = sp.getString("pin_salt", null);
        String expected = sp.getString("pin_hash", null);
        if (saltB64 == null || expected == null) return false;
        byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
        return expected.equals(hash(pin, salt));
    }

    private static String hash(String pin, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            md.update(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(md.digest(), Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}
