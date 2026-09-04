package cn.lineai.resource;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

/**
 * Reads Material You (Android 12+) system accent colors without pulling in the Material
 * Components library.
 *
 * <p>On API 31+ the framework exposes wallpaper-derived accent colors such as
 * {@code android.R.color.system_accent1_400}. On older releases — or when the resource
 * cannot be resolved — {@code 0} is returned so the theme layer can fall back to the
 * static dark scheme.</p>
 */
public final class DynamicColors {

    private DynamicColors() {
    }

    /**
     * Returns the system accent color used as the Dynamic Color seed, or {@code 0} when
     * unavailable.
     */
    public static int getSystemAccentColor(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return 0;
        }
        try {
            return context.getResources().getColor(android.R.color.system_accent1_400, context.getTheme());
        } catch (Resources.NotFoundException ignored) {
            return 0;
        }
    }
}
