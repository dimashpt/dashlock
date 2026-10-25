package id.sooon.dashlock;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

/** Terapkan bahasa dari {@link Prefs} ke konteks (default Inggris). */
final class LocaleHelper {
    private LocaleHelper() { }

    static Context wrap(Context context) {
        Locale locale = localeFor(Prefs.language(context));
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.createConfigurationContext(config);
        }
        Resources res = context.getResources();
        res.updateConfiguration(config, res.getDisplayMetrics());
        return context;
    }

    /** Kode legacy `in` agar resource `values-in` terpilih di Android 7–8. */
    private static Locale localeFor(String lang) {
        if (Prefs.LANG_ID.equals(lang)) {
            return new Locale("in");
        }
        return Locale.ENGLISH;
    }
}
