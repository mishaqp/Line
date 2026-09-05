package cn.lineai.mvp;

import android.content.Context;
import cn.lineai.R;
import cn.lineai.model.SheetOption;
import java.util.ArrayList;

final class OverlayActionController {
    interface Host {
        boolean isViewAttached();

        void showSheet(String title, ArrayList<SheetOption> options);

        void hideOverlays();

        void showScreen(String screenId);

        void render();

        void exportCurrentChat();

        void enterMessageSelectMode();
    }

    private final Context context;
    private final ProjectWorkspaceController projectWorkspaceController;
    private final FileOperationController fileOperationController;
    private final PermissionModeController permissionModeController;
    private final ContextCompactionController contextCompactionController;
    private final ChatInteractionController chatInteractionController;
    private final LineCodeArchiveController lineCodeArchiveController;
    private final Host host;

    OverlayActionController(
            Context context,
            ProjectWorkspaceController projectWorkspaceController,
            FileOperationController fileOperationController,
            PermissionModeController permissionModeController,
            ContextCompactionController contextCompactionController,
            ChatInteractionController chatInteractionController,
            LineCodeArchiveController lineCodeArchiveController,
            Host host
    ) {
        this.context = context.getApplicationContext();
        this.projectWorkspaceController = projectWorkspaceController;
        this.fileOperationController = fileOperationController;
        this.permissionModeController = permissionModeController;
        this.contextCompactionController = contextCompactionController;
        this.chatInteractionController = chatInteractionController;
        this.lineCodeArchiveController = lineCodeArchiveController;
        this.host = host;
    }

    void handleDialogInput(String actionId, String value) {
        String id = actionId == null ? "" : actionId;
        if (projectWorkspaceController.handleDialogInput(id, value)) {
            return;
        }
        if (id.startsWith("file:create_file:")) {
            fileOperationController.createFileFromInput(id.substring("file:create_file:".length()), value);
            return;
        }
        if (id.startsWith("file:create_folder:")) {
            fileOperationController.createFolderFromInput(id.substring("file:create_folder:".length()), value);
            return;
        }
        if (id.startsWith("file:rename:")) {
            fileOperationController.renameFileNodeFromInput(id.substring("file:rename:".length()), value);
        }
    }

    void handleDialogConfirmed(String actionId) {
        String id = actionId == null ? "" : actionId;
        if (id.startsWith("file:delete:")) {
            fileOperationController.deleteFileNode(id.substring("file:delete:".length()));
            return;
        }
        if ("data:import_linecode".equals(id)) {
            lineCodeArchiveController.confirmImport();
        }
    }

    void showMoreActions() {
        if (!host.isViewAttached()) {
            return;
        }
        ArrayList<SheetOption> options = new ArrayList<>();
        options.add(new SheetOption("settings",
                context.getString(R.string.screen_settings_title),
                context.getString(R.string.sheet_more_settings_desc), false));
        options.add(new SheetOption("export",
                context.getString(R.string.sheet_more_export),
                context.getString(R.string.sheet_more_export_desc), false));
        options.add(new SheetOption("select_export",
                context.getString(R.string.sheet_more_select_export),
                context.getString(R.string.sheet_more_select_export_desc), false));
        options.add(new SheetOption("compact",
                context.getString(R.string.sheet_more_compact),
                context.getString(R.string.sheet_more_compact_desc), false));
        options.add(new SheetOption("clear",
                context.getString(R.string.sheet_more_clear),
                context.getString(R.string.sheet_more_clear_desc), false));
        host.showSheet(context.getString(R.string.common_more), options);
    }

    void handleSheetOption(String id) {
        if (projectWorkspaceController.handleSheetOption(id)) {
            return;
        }
        if (id != null && id.startsWith("file:create_file:")) {
            fileOperationController.requestCreateFile(id.substring("file:create_file:".length()));
            return;
        } else if (id != null && id.startsWith("file:create_folder:")) {
            fileOperationController.requestCreateFolder(id.substring("file:create_folder:".length()));
            return;
        } else if (id != null && id.startsWith("file:copy:")) {
            fileOperationController.copyFileNode(id.substring("file:copy:".length()));
        } else if (id != null && id.startsWith("file:paste:")) {
            fileOperationController.pasteFileNode(id.substring("file:paste:".length()));
            return;
        } else if (id != null && id.startsWith("file:rename:")) {
            fileOperationController.requestRenameFileNode(id.substring("file:rename:".length()));
            return;
        } else if (id != null && id.startsWith("file:delete:")) {
            fileOperationController.requestDeleteFileNode(id.substring("file:delete:".length()));
            return;
        } else if (permissionModeController.applyPermissionModeOption(id)) {
            // Handled above.
        } else if ("settings".equals(id)) {
            host.showScreen("settings");
        } else if ("compact".equals(id)) {
            contextCompactionController.showCompactConfirmation();
            return;
        } else if ("compact:confirm".equals(id)) {
            contextCompactionController.startManualContextCompaction();
        } else if ("compact:cancel".equals(id)) {
            // The bottom sheet is closed below.
        } else if ("clear".equals(id)) {
            chatInteractionController.clearCurrentConversation();
        } else if ("export".equals(id)) {
            host.exportCurrentChat();
        } else if ("select_export".equals(id)) {
            host.enterMessageSelectMode();
        }
        if (!"settings".equals(id)) {
            host.hideOverlays();
        }
        host.render();
    }
}
