package cn.lineai.mvp;

public interface SettingsController extends AiBehaviorSettingsController,
        InputSettingsController,
        OutputSettingsController,
        ThemeSettingsController,
        McpSettingsController,
        ArchiveController,
        StorageController {
    void onMoreClick();

    void onSettingsItemSelected(String id);

    boolean isFullAccessEnabled();

    void onFullAccessChanged(boolean enabled);
}
