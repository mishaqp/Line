package cn.lineai.ui.component;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.widget.FrameLayout;
import cn.lineai.data.db.LineCodeDatabase;
import cn.lineai.data.repository.PhoneControlRepository;
import cn.lineai.data.repository.SettingsRepository;
import cn.lineai.service.LineCodeAccessibilityService;
import cn.lineai.ui.model.PhoneControlSettingsRepository;

public final class PhoneControlScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onOpenAccessibilitySettings();

        void onPermissionEnabledChanged(String permissionId, boolean enabled);

        boolean isPermissionEnabled(String permissionId);

        void onSetPermissionEnabled(String permissionId, boolean enabled);

        void onAcceptDisclaimer();
    }

    private final PhoneControlHostView hostView;
    private boolean attachedOnce;

    public PhoneControlScreenView(Context context, boolean accessibilityEnabled,
                                  boolean disclaimerAccepted, Listener listener) {
        super(context);

        Context appContext = context.getApplicationContext();
        PhoneControlRepository backend = new PhoneControlRepository(
                new SettingsRepository(LineCodeDatabase.getInstance(appContext)),
                () -> LineCodeAccessibilityService.isServiceEnabled(appContext)
        );
        PhoneControlSettingsRepository repository = new PhoneControlSettingsRepository() {
            @Override
            public boolean isAccessibilityEnabled() {
                return backend.isAccessibilityEnabled();
            }

            @Override
            public boolean isDisclaimerAccepted() {
                return backend.isDisclaimerAccepted();
            }

            @Override
            public void setDisclaimerAccepted(boolean accepted) {
                if (accepted) {
                    listener.onAcceptDisclaimer();
                } else {
                    backend.setDisclaimerAccepted(false);
                }
            }

            @Override
            public boolean isPermissionEnabled(String permissionId) {
                return backend.isPermissionEnabled(permissionId);
            }

            @Override
            public void setPermissionEnabled(String permissionId, boolean enabled) {
                listener.onSetPermissionEnabled(permissionId, enabled);
            }
        };

        hostView = new PhoneControlHostView(
                context,
                repository,
                new PhoneControlHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }

                    @Override
                    public void onOpenAccessibilitySettings() {
                        openAccessibilitySettings(context, listener);
                    }
                }
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (attachedOnce) {
            hostView.refresh();
            return;
        }
        attachedOnce = true;
    }

    private static void openAccessibilitySettings(Context context, Listener listener) {
        listener.onOpenAccessibilitySettings();
        try {
            context.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception ignored) {
        }
    }
}
