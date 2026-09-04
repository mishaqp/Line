package cn.lineai.mvp;

import cn.lineai.data.repository.ToolSettingsRepository;
import cn.lineai.model.ChatMode;
import cn.lineai.model.SheetOption;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

public final class PermissionModeControllerTest {
    @Test
    public void showPermissionSheetMarksCurrentModeAndStorageStatus() {
        Fixture fixture = new Fixture();
        fixture.permissionStore.permissionMode = ToolSettingsRepository.PERMISSION_CONFIRM;
        fixture.host.externalStorageAccess = false;
        fixture.host.storageMessage = "需要授权";

        fixture.controller.showPermissionSheet();

        Assert.assertEquals(5, fixture.host.options.size());
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_CONFIRM, fixture.host.options.get(1).getId());
        Assert.assertTrue(fixture.host.options.get(1).isSelected());
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_FULL_ACCESS, fixture.host.options.get(2).getId());
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_READONLY, fixture.host.options.get(3).getId());
        Assert.assertEquals("storage:manage_all_files", fixture.host.options.get(4).getId());
        Assert.assertEquals("需要授权", fixture.host.options.get(4).getDescription());
        Assert.assertFalse(fixture.host.options.get(4).isSelected());
    }

    @Test
    public void fullAccessIsSelectableAndKeepsAgentMode() {
        Fixture fixture = new Fixture();
        fixture.chatModeStore.mode = ChatMode.CHAT;

        Assert.assertTrue(fixture.controller.applyPermissionModeOption(ToolSettingsRepository.PERMISSION_FULL_ACCESS));

        Assert.assertEquals(ToolSettingsRepository.PERMISSION_FULL_ACCESS, fixture.permissionStore.permissionMode);
        // 完全访问不属于只读，应该把会话切回 Agent 并记住该权限。
        Assert.assertEquals(ChatMode.AGENT, fixture.chatModeStore.mode);
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_FULL_ACCESS, fixture.chatModeStore.rememberedPermission);
    }

    @Test
    public void fullAccessAliasesAreAccepted() {
        Fixture fixture = new Fixture();

        Assert.assertTrue(fixture.controller.applyPermissionModeOption("full"));
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_FULL_ACCESS, fixture.permissionStore.permissionMode);
    }

    @Test
    public void readonlyPermissionSwitchesChatModeToChat() {
        Fixture fixture = new Fixture();
        fixture.chatModeStore.mode = ChatMode.AGENT;

        Assert.assertTrue(fixture.controller.applyPermissionModeOption(ToolSettingsRepository.PERMISSION_READONLY));

        Assert.assertEquals(ToolSettingsRepository.PERMISSION_READONLY, fixture.permissionStore.permissionMode);
        Assert.assertEquals(ChatMode.CHAT, fixture.chatModeStore.mode);
        Assert.assertEquals("", fixture.chatModeStore.rememberedPermission);
    }

    @Test
    public void nonReadonlyPermissionLeavesAgentModeAndRemembersPermission() {
        Fixture fixture = new Fixture();
        fixture.chatModeStore.mode = ChatMode.AGENT;

        Assert.assertTrue(fixture.controller.applyPermissionModeOption(ToolSettingsRepository.PERMISSION_CONFIRM));

        Assert.assertEquals(ToolSettingsRepository.PERMISSION_CONFIRM, fixture.permissionStore.permissionMode);
        Assert.assertEquals(ChatMode.AGENT, fixture.chatModeStore.mode);
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_CONFIRM, fixture.chatModeStore.rememberedPermission);
    }

    @Test
    public void nonReadonlyPermissionMovesChatModeBackToAgent() {
        Fixture fixture = new Fixture();
        fixture.chatModeStore.mode = ChatMode.CHAT;

        fixture.controller.applyPermissionModeOption(ToolSettingsRepository.PERMISSION_AUTO);

        Assert.assertEquals(ToolSettingsRepository.PERMISSION_AUTO, fixture.permissionStore.permissionMode);
        Assert.assertEquals(ChatMode.AGENT, fixture.chatModeStore.mode);
    }

    @Test
    public void legacyAliasesAreAccepted() {
        Fixture fixture = new Fixture();

        Assert.assertTrue(fixture.controller.applyPermissionModeOption("ask"));
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_CONFIRM, fixture.permissionStore.permissionMode);

        Assert.assertTrue(fixture.controller.applyPermissionModeOption("manual"));
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_READONLY, fixture.permissionStore.permissionMode);
    }

    @Test
    public void unknownOptionIsIgnored() {
        Fixture fixture = new Fixture();

        Assert.assertFalse(fixture.controller.applyPermissionModeOption("unknown"));
        Assert.assertEquals(ToolSettingsRepository.PERMISSION_AUTO, fixture.permissionStore.permissionMode);
    }

    private static final class Fixture {
        private final FakePermissionStore permissionStore = new FakePermissionStore();
        private final FakeChatModeStore chatModeStore = new FakeChatModeStore();
        private final FakeHost host = new FakeHost();
        private final PermissionModeController controller = new PermissionModeController(
                permissionStore,
                chatModeStore,
                host
        );
    }

    private static final class FakePermissionStore implements PermissionModeController.PermissionStore {
        private String permissionMode = ToolSettingsRepository.PERMISSION_AUTO;

        @Override
        public String getPermissionMode() {
            return permissionMode;
        }

        @Override
        public void setPermissionMode(String mode) {
            permissionMode = mode;
        }
    }

    private static final class FakeChatModeStore implements PermissionModeController.ChatModeStore {
        private String mode = ChatMode.AGENT;
        private String rememberedPermission = "";

        @Override
        public String getMode() {
            return mode;
        }

        @Override
        public void setModeOnly(String mode) {
            this.mode = mode;
        }

        @Override
        public void rememberRestorablePermission(String mode) {
            rememberedPermission = mode;
        }
    }

    private static final class FakeHost implements PermissionModeController.Host {
        private boolean externalStorageAccess;
        private String storageMessage = "";
        private ArrayList<SheetOption> options = new ArrayList<>();

        @Override
        public boolean hasExternalStorageAccess() {
            return externalStorageAccess;
        }

        @Override
        public String storagePermissionMessage() {
            return storageMessage;
        }

        @Override
        public void showPermissionSheet(ArrayList<SheetOption> options) {
            this.options = options;
        }
    }
}
