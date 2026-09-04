package cn.lineai.mvp;

import cn.lineai.model.ThemeSettingsState;
import java.util.Map;

public interface ThemeSettingsController {
    ThemeSettingsState getThemeSettings();

    void onThemeModeChanged(String mode);

    void onCustomThemeColorsSaved(Map<String, String> colors);

    String getChatScaleMode();

    void onChatScaleModeChanged(String mode);
}
