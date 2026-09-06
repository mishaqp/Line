package cn.lineai.ui.component;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import cn.lineai.R;
import cn.lineai.model.KeepAliveSettings;
import cn.lineai.ui.model.KeepAliveSettingsRepository;
import cn.lineai.ui.model.KeepAliveStoredSettings;

public final class KeepAliveSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
        void onSettingsChanged();
        KeepAliveSettings onLoadSettings();
        void onSetWakeLockEnabled(boolean enabled);
        void onSetForegroundEnabled(boolean enabled);
        void onSetFakeAudioEnabled(boolean enabled);
        void onUpdateService();
        void onUpdateServiceStatus(String status);
        void onRequestIgnoreBatteryOptimizations();
    }

    private final Listener listener;

    public KeepAliveSettingsScreenView(Context context, Listener listener) {
        this(context, listener, null);
    }

    public KeepAliveSettingsScreenView(
            Context context,
            Listener listener,
            PermissionUiHelper permissionUiHelper
    ) {
        super(context);
        this.listener = listener;

        KeepAliveSettingsRepository repository = new KeepAliveSettingsRepository() {
            @Override
            public KeepAliveStoredSettings loadSettings() {
                KeepAliveSettings settings = listener.onLoadSettings();
                if (settings == null) {
                    return null;
                }
                return new KeepAliveStoredSettings(
                        settings.wakeLockEnabled,
                        settings.foregroundEnabled,
                        settings.fakeAudioEnabled
                );
            }

            @Override
            public void setWakeLockEnabled(boolean enabled) {
                listener.onSetWakeLockEnabled(enabled);
            }

            @Override
            public void setForegroundEnabled(boolean enabled) {
                listener.onSetForegroundEnabled(enabled);
            }

            @Override
            public void setFakeAudioEnabled(boolean enabled) {
                listener.onSetFakeAudioEnabled(enabled);
            }

            @Override
            public void updateService() {
                listener.onUpdateService();
            }

            @Override
            public void notifySettingsChanged() {
                listener.onSettingsChanged();
            }

            @Override
            public boolean hasPostNotificationsPermission() {
                return permissionUiHelper == null || permissionUiHelper.hasPostNotificationsPermission();
            }

            @Override
            public boolean isIgnoringBatteryOptimizations() {
                return KeepAliveSettingsScreenView.isIgnoringBatteryOptimizations(context);
            }
        };

        addView(
                new KeepAliveSettingsHostView(
                        context,
                        repository,
                        new KeepAliveSettingsHostView.Listener() {
                            @Override
                            public void onBack() {
                                listener.onBack();
                            }

                            @Override
                            public void onRequestPostNotifications() {
                                if (permissionUiHelper == null || permissionUiHelper.hasPostNotificationsPermission()) {
                                    return;
                                }
                                permissionUiHelper.requestPostNotificationsPermission();
                                Toast.makeText(
                                        context,
                                        R.string.screen_keep_alive_notification_permission_hint,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                            @Override
                            public void onOpenBatteryOptimizationSettings() {
                                listener.onRequestIgnoreBatteryOptimizations();
                            }
                        }
                ),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static boolean isIgnoringBatteryOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
            }
        }
        return true;
    }

    public void updateStatus(String status) {
        listener.onUpdateServiceStatus(status);
    }
}
