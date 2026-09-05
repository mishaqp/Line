package cn.lineai.mvp;

import cn.lineai.data.repository.FileTreeStore;
import cn.lineai.data.repository.IpcFileTreeStore;
import cn.lineai.data.repository.SshFileTreeStore;
import cn.lineai.model.SheetOption;
import java.util.ArrayList;

public final class FileOperationController {
    interface BackgroundRunner extends cn.lineai.mvp.BackgroundRunner {
    }

    interface FileStore {
        void createFile(String parentPath, String name) throws Exception;

        void createDirectory(String parentPath, String name) throws Exception;

        void rename(String path, String newName) throws Exception;

        void delete(String path) throws Exception;

        void copyInto(String sourcePath, String targetDirectoryPath) throws Exception;
    }

    interface Host extends CoordinatorHost {
        boolean isSshExecutionMode();

        boolean isTerminalProviderExecutionMode();

        void showInputDialog(String title, String message, String initialValue, String actionId);

        void showConfirmationDialog(String title, String message, String confirmLabel, boolean danger, String actionId);

        void showFileActionDialog(String title, String subtitle, ArrayList<SheetOption> options);

        void addExpandedPath(String path);

        void refreshSshDirectoryAfterFileOperation(String path);

        void refreshIpcDirectoryAfterFileOperation(String path);

        void showNotice(String text);
    }

    interface UiDispatcher extends cn.lineai.mvp.UiDispatcher {
    }

    private static final class LocalFileStore implements FileStore {
        private final FileTreeStore repository;

        LocalFileStore(FileTreeStore repository) {
            this.repository = repository;
        }

        @Override
        public void createFile(String parentPath, String name) throws Exception {
            repository.createFile(parentPath, name);
        }

        @Override
        public void createDirectory(String parentPath, String name) throws Exception {
            repository.createDirectory(parentPath, name);
        }

        @Override
        public void rename(String path, String newName) throws Exception {
            repository.rename(path, newName);
        }

        @Override
        public void delete(String path) throws Exception {
            repository.delete(path);
        }

        @Override
        public void copyInto(String sourcePath, String targetDirectoryPath) throws Exception {
            repository.copyInto(sourcePath, targetDirectoryPath);
        }
    }

    private static final class SshFileStore implements FileStore {
        private final SshFileTreeStore repository;

        SshFileStore(SshFileTreeStore repository) {
            this.repository = repository;
        }

        @Override
        public void createFile(String parentPath, String name) throws Exception {
            repository.createFile(parentPath, name);
        }

        @Override
        public void createDirectory(String parentPath, String name) throws Exception {
            repository.createDirectory(parentPath, name);
        }

        @Override
        public void rename(String path, String newName) throws Exception {
            repository.rename(path, newName);
        }

        @Override
        public void delete(String path) throws Exception {
            repository.delete(path);
        }

        @Override
        public void copyInto(String sourcePath, String targetDirectoryPath) throws Exception {
            repository.copyInto(sourcePath, targetDirectoryPath);
        }
    }

    private static final class IpcFileStore implements FileStore {
        private final IpcFileTreeStore repository;

        IpcFileStore(IpcFileTreeStore repository) {
            this.repository = repository;
        }

        @Override
        public void createFile(String parentPath, String name) throws Exception {
            repository.createFile(parentPath, name);
        }

        @Override
        public void createDirectory(String parentPath, String name) throws Exception {
            repository.createDirectory(parentPath, name);
        }

        @Override
        public void rename(String path, String newName) throws Exception {
            repository.rename(path, newName);
        }

        @Override
        public void delete(String path) throws Exception {
            repository.delete(path);
        }

        @Override
        public void copyInto(String sourcePath, String targetDirectoryPath) throws Exception {
            repository.copyInto(sourcePath, targetDirectoryPath);
        }
    }

    private final ExecutionModeFileStoreRegistry fileStoreRegistry;
    private final Host host;
    private final BackgroundRunner backgroundRunner;
    private final UiDispatcher uiDispatcher;
    private String clipboardPath = "";
    private String clipboardName = "";
    private boolean clipboardSsh;
    private boolean clipboardIpc;

    public FileOperationController(
            FileTreeStore fileTreeRepository,
            SshFileTreeStore sshFileTreeRepository,
            IpcFileTreeStore ipcFileTreeRepository,
            Host host,
            BackgroundRunner backgroundRunner,
            UiDispatcher uiDispatcher
    ) {
        this(
                new LocalFileStore(fileTreeRepository),
                new SshFileStore(sshFileTreeRepository),
                new IpcFileStore(ipcFileTreeRepository),
                host,
                backgroundRunner,
                uiDispatcher
        );
    }

    FileOperationController(
            FileStore localFileStore,
            FileStore sshFileStore,
            FileStore ipcFileStore,
            Host host,
            BackgroundRunner backgroundRunner,
            UiDispatcher uiDispatcher
    ) {
        this.fileStoreRegistry = new ExecutionModeFileStoreRegistry();
        this.fileStoreRegistry.register(ExecutionModeFileStoreRegistry.MODE_LOCAL, localFileStore);
        this.fileStoreRegistry.register(ExecutionModeFileStoreRegistry.MODE_SSH, sshFileStore);
        this.fileStoreRegistry.register(ExecutionModeFileStoreRegistry.MODE_IPC, ipcFileStore);
        this.host = host;
        this.backgroundRunner = backgroundRunner;
        this.uiDispatcher = uiDispatcher;
    }

    public String clipboardName() {
        return clipboardName;
    }

    public boolean canPasteInto(boolean directory) {
        if (!directory || clipboardPath.length() == 0) {
            return false;
        }
        if (host.isSshExecutionMode()) {
            return clipboardSsh;
        }
        if (host.isTerminalProviderExecutionMode()) {
            return clipboardIpc;
        }
        return !clipboardSsh && !clipboardIpc;
    }

    public void showFileNodeActions(String path, String name, boolean directory, boolean root) {
        if (path == null || path.length() == 0) {
            return;
        }
        ArrayList<SheetOption> options = new ArrayList<>();
        if (directory) {
            options.add(new SheetOption("file:create_file:" + path, "Новый файл", path, false));
            options.add(new SheetOption("file:create_folder:" + path, "Новая папка", path, false));
            if (canPasteInto(true)) {
                options.add(new SheetOption("file:paste:" + path, "Вставить", clipboardName, false));
            }
            if (!root) {
                options.add(new SheetOption("file:copy:" + path, "Копировать", name, false));
                options.add(new SheetOption("file:rename:" + path, "Переименовать", name, false));
                options.add(new SheetOption("file:delete:" + path, "Удалить", path, false));
            }
        } else {
            options.add(new SheetOption("file:copy:" + path, "Копировать", name, false));
            options.add(new SheetOption("file:rename:" + path, "Переименовать", name, false));
            options.add(new SheetOption("file:delete:" + path, "Удалить", path, false));
        }
        if (!options.isEmpty()) {
            host.showFileActionDialog(
                    root ? "Корень рабочей области" : (name == null || name.length() == 0 ? "Действие с файлом" : name),
                    path,
                    options
            );
        }
    }

    public void requestCreateFile(String parentPath) {
        host.showInputDialog("Новый файл", parentPath, "", "file:create_file:" + parentPath);
    }

    public void requestCreateFolder(String parentPath) {
        host.showInputDialog("Новая папка", parentPath, "", "file:create_folder:" + parentPath);
    }

    public void requestRenameFileNode(String path) {
        host.showInputDialog("Переименовать", path, host.basename(path), "file:rename:" + path);
    }

    public void requestDeleteFileNode(String path) {
        host.showConfirmationDialog(
                "Подтвердить удаление",
                "Удалить «" + host.basename(path) + "»?\n\nЭто нельзя отменить.\n\n" + path,
                "Удалить",
                true,
                "file:delete:" + path
        );
    }

    public void createFileFromInput(String parentPath, String name) {
        runFileOperation(parentPath, () -> currentFileStore().createFile(parentPath, name));
    }

    public void createFolderFromInput(String parentPath, String name) {
        runFileOperation(parentPath, () -> currentFileStore().createDirectory(parentPath, name));
    }

    public void renameFileNodeFromInput(String path, String newName) {
        runFileOperation(host.parentPath(path), () -> currentFileStore().rename(path, newName));
    }

    public void deleteFileNode(String path) {
        runFileOperation(host.parentPath(path), () -> currentFileStore().delete(path));
    }

    public void copyFileNode(String path) {
        clipboardPath = path == null ? "" : path;
        clipboardName = host.basename(path);
        clipboardSsh = host.isSshExecutionMode();
        clipboardIpc = host.isTerminalProviderExecutionMode();
    }

    public void pasteFileNode(String targetDirectoryPath) {
        if (clipboardPath.length() == 0) {
            return;
        }
        if (host.isSshExecutionMode() && !clipboardSsh) {
            return;
        }
        if (host.isTerminalProviderExecutionMode() && !clipboardIpc) {
            return;
        }
        if (!host.isSshExecutionMode() && !host.isTerminalProviderExecutionMode() && (clipboardSsh || clipboardIpc)) {
            return;
        }
        runFileOperation(targetDirectoryPath, () -> currentFileStore().copyInto(clipboardPath, targetDirectoryPath));
    }

    private FileStore currentFileStore() {
        return fileStoreRegistry.get(ExecutionModeFileStoreRegistry.resolveMode(host));
    }

    private void runFileOperation(String expandedPath, FileOperation operation) {
        backgroundRunner.execute("linecode-file-operation", () -> {
            try {
                operation.run();
                uiDispatcher.post(() -> {
                    if (expandedPath != null && expandedPath.length() > 0) {
                        host.addExpandedPath(expandedPath);
                    }
                    if (host.isSshExecutionMode()) {
                        host.refreshSshDirectoryAfterFileOperation(expandedPath);
                    }
                    if (host.isTerminalProviderExecutionMode()) {
                        host.refreshIpcDirectoryAfterFileOperation(expandedPath);
                    }
                    host.render();
                });
            } catch (Exception e) {
                uiDispatcher.post(() -> host.showNotice("Не удалось выполнить операцию с файлом: " + e.getMessage()));
            }
        });
    }

    private interface FileOperation {
        void run() throws Exception;
    }
}
