package cn.lineai.resource;

/**
 * Provides system configuration information without requiring Android Context.
 * Decouples data layer from Android framework.
 */
public interface SystemConfigProvider {
    boolean isDarkModeEnabled();
    int getSdkInt();

    /**
     * Material You system accent color for the Dynamic Color theme.
     * Returns {@code 0} when the platform does not expose one (API &lt; 31),
     * so callers fall back to the static dark scheme.
     */
    int getDynamicAccentColor();

    String getFilesDirPath();
    String getDatabasePath(String name);
    String getExternalFilesDirPath();
}
