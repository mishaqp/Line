package cn.lineai.mvp;

import android.content.Context;
import cn.lineai.R;
import cn.lineai.data.repository.ChatModeRepository;
import cn.lineai.data.repository.ToolSettingsRepository;
import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.ChatMode;
import cn.lineai.model.SheetOption;
import java.util.ArrayList;

public final class PermissionModeController {
    interface Host {
        boolean hasExternalStorageAccess();

        String storagePermissionMessage();

        void showPermissionSheet(ArrayList<SheetOption> options);
    }

    interface PermissionStore {
        String getPermissionMode();

        void setPermissionMode(String mode);
    }

    interface ChatModeStore {
        String getMode();

        void setModeOnly(String mode);

        void rememberRestorablePermission(String mode);
    }

    private static final class ToolSettingsPermissionStore implements PermissionStore {
        private final ToolSettingsStore repository;

        ToolSettingsPermissionStore(ToolSettingsStore repository) {
            this.repository = repository;
        }

        @Override
        public String getPermissionMode() {
            return repository.getPermissionMode();
        }

        @Override
        public void setPermissionMode(String mode) {
            repository.setPermissionMode(mode);
        }
    }

    private static final class RepositoryChatModeStore implements ChatModeStore {
        private final ChatModeRepository repository;

        RepositoryChatModeStore(ChatModeRepository repository) {
            this.repository = repository;
        }

        @Override
        public String getMode() {
            return repository.getMode();
        }

        @Override
        public void setModeOnly(String mode) {
            repository.setModeOnly(mode);
        }

        @Override
        public void rememberRestorablePermission(String mode) {
            repository.rememberRestorablePermission(mode);
        }
    }

    private final PermissionStore permissionStore;
    private final ChatModeStore chatModeStore;
    private final Context context;
    private final Host host;

    public PermissionModeController(
            ToolSettingsStore toolSettingsRepository,
            ChatModeRepository chatModeRepository,
            Context context,
            Host host
    ) {
        this(
                new ToolSettingsPermissionStore(toolSettingsRepository),
                new RepositoryChatModeStore(chatModeRepository),
                context,
                host
        );
    }

    PermissionModeController(PermissionStore permissionStore, ChatModeStore chatModeStore, Host host) {
        this(permissionStore, chatModeStore, null, host);
    }

    PermissionModeController(PermissionStore permissionStore, ChatModeStore chatModeStore, Context context, Host host) {
        this.permissionStore = permissionStore;
        this.chatModeStore = chatModeStore;
        this.context = context == null ? null : context.getApplicationContext();
        this.host = host;
    }

    public void showPermissionSheet() {
        String permissionMode = permissionStore.getPermissionMode();
        ArrayList<SheetOption> options = new ArrayList<>();
        options.add(new SheetOption(
                ToolSettingsRepository.PERMISSION_AUTO,
                localizedString(R.string.permission_mode_auto, "自动"),
                localizedString(R.string.permission_mode_auto_desc, "自动执行已启用工具，危险工具按策略确认"),
                ToolSettingsRepository.PERMISSION_AUTO.equals(permissionMode)
        ));
        options.add(new SheetOption(
                ToolSettingsRepository.PERMISSION_CONFIRM,
                localizedString(R.string.permission_mode_confirm, "确认"),
                localizedString(R.string.permission_mode_confirm_desc, "危险操作需要确认后执行"),
                ToolSettingsRepository.PERMISSION_CONFIRM.equals(permissionMode)
        ));
        options.add(new SheetOption(
                ToolSettingsRepository.PERMISSION_FULL_ACCESS,
                localizedString(R.string.permission_mode_full_access, "完全访问"),
                localizedString(R.string.permission_mode_full_access_desc, "所有已启用工具直接执行，不再弹出任何确认（含删除、Shell 与 Root）"),
                ToolSettingsRepository.PERMISSION_FULL_ACCESS.equals(permissionMode)
        ));
        options.add(new SheetOption(
                ToolSettingsRepository.PERMISSION_READONLY,
                localizedString(R.string.permission_mode_readonly, "只读"),
                localizedString(R.string.permission_mode_readonly_desc, "仅允许读取、搜索和列目录，禁止写入与 Shell"),
                ToolSettingsRepository.PERMISSION_READONLY.equals(permissionMode)
        ));
        options.add(new SheetOption(
                "storage:manage_all_files",
                localizedString(R.string.permission_mode_manage_all_files, "管理所有文件权限"),
                host.hasExternalStorageAccess()
                        ? localizedString(R.string.permission_mode_storage_granted, "已授权，可访问文件存储")
                        : host.storagePermissionMessage(),
                host.hasExternalStorageAccess()
        ));
        host.showPermissionSheet(options);
    }

    private String localizedString(int resId, String fallback) {
        return context == null ? fallback : context.getString(resId);
    }

    public boolean isPermissionModeOption(String id) {
        return ToolSettingsRepository.PERMISSION_AUTO.equals(id)
                || ToolSettingsRepository.PERMISSION_CONFIRM.equals(id)
                || ToolSettingsRepository.PERMISSION_FULL_ACCESS.equals(id)
                || ToolSettingsRepository.PERMISSION_READONLY.equals(id)
                || "ask".equals(id)
                || "workspace".equals(id)
                || "manual".equals(id);
    }

    public boolean applyPermissionModeOption(String id) {
        if (!isPermissionModeOption(id)) {
            return false;
        }
        String permissionMode = ToolSettingsRepository.normalizePermissionMode(id);
        permissionStore.setPermissionMode(permissionMode);
        if (ToolSettingsRepository.PERMISSION_READONLY.equals(permissionMode)) {
            chatModeStore.setModeOnly(ChatMode.CHAT);
            return true;
        }
        chatModeStore.rememberRestorablePermission(permissionMode);
        if (ChatMode.CHAT.equals(chatModeStore.getMode())) {
            chatModeStore.setModeOnly(ChatMode.AGENT);
        }
        return true;
    }
}
