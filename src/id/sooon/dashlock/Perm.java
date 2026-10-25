package id.sooon.dashlock;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/** Cek izin overlay dan layanan aksesibilitas DashLock. */
final class Perm {
    private Perm() { }

    static boolean overlay(Context c) {
        return Settings.canDrawOverlays(c);
    }

    static boolean accessibility(Context c) {
        ComponentName mine = new ComponentName(c, LockAccessibilityService.class);
        if (isAccessibilityServiceEnabledInSettings(c, mine)) return true;
        return isAccessibilityServiceEnabledInManager(c, mine);
    }

    /** Head unit sering hanya konsisten lewat Settings.Secure (bukan daftar AccessibilityManager). */
    private static boolean isAccessibilityServiceEnabledInSettings(Context c, ComponentName mine) {
        try {
            int on = Settings.Secure.getInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (on != 1) return false;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        String enabled = Settings.Secure.getString(
                c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        String flat = mine.flattenToString();
        while (splitter.hasNext()) {
            String entry = splitter.next();
            if (TextUtils.isEmpty(entry)) continue;
            if (entry.equals(flat)) return true;
            ComponentName cn = ComponentName.unflattenFromString(entry);
            if (mine.equals(cn)) return true;
        }
        return false;
    }

    private static boolean isAccessibilityServiceEnabledInManager(Context c, ComponentName mine) {
        AccessibilityManager am = (AccessibilityManager) c.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null || !am.isEnabled()) return false;
        List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo info : list) {
            if (info == null) continue;
            String id = info.getId();
            if (!TextUtils.isEmpty(id)) {
                ComponentName cn = ComponentName.unflattenFromString(id);
                if (mine.equals(cn)) return true;
            }
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null) {
                ComponentName enabled = new ComponentName(
                        info.getResolveInfo().serviceInfo.packageName,
                        info.getResolveInfo().serviceInfo.name);
                if (mine.equals(enabled)) return true;
            }
        }
        return false;
    }

    static boolean ready(Context c) {
        if (!overlay(c) || !accessibility(c)) return false;
        return !Prefs.unlockRequirePin(c) || Prefs.hasPin(c);
    }
}
