package cn.lineai.mvp;

import cn.lineai.data.repository.ProjectRecord;
import cn.lineai.data.repository.ToolSettingsRepository;
import cn.lineai.model.SheetOption;
import cn.lineai.workspace.WorkspacePaths;
import java.util.ArrayList;
import java.util.List;

public final class ProjectSheetController {
    static final class ProjectSheet {
        private final String title;
        private final ArrayList<SheetOption> options;

        ProjectSheet(String title, ArrayList<SheetOption> options) {
            this.title = title;
            this.options = options;
        }

        String getTitle() {
            return title;
        }

        ArrayList<SheetOption> getOptions() {
            return options;
        }
    }

    interface Host {
        String executionMode();
        boolean isTermuxSshHost();
        boolean hasExternalStorageAccess();
        String storagePermissionMessage();
    }

    interface ProjectStore {
        ProjectRecord getSelectedProject(String executionMode);
        List<ProjectRecord> getProjects(String executionMode);
    }

    private static final class RepositoryProjectStore implements ProjectStore {
        private final cn.lineai.data.repository.ProjectStore projectStore;

        RepositoryProjectStore(cn.lineai.data.repository.ProjectStore projectStore) {
            this.projectStore = projectStore;
        }

        @Override
        public ProjectRecord getSelectedProject(String executionMode) {
            return projectStore.getSelectedProject(executionMode);
        }

        @Override
        public List<ProjectRecord> getProjects(String executionMode) {
            return projectStore.getProjects(executionMode);
        }
    }

    private final ProjectStore projectStore;
    private final Host host;

    public ProjectSheetController(cn.lineai.data.repository.ProjectStore projectStore, Host host) {
        this(new RepositoryProjectStore(projectStore), host);
    }

    ProjectSheetController(ProjectStore projectStore, Host host) {
        this.projectStore = projectStore;
        this.host = host;
    }

    public ProjectSheet buildProjectSheet() {
        String executionMode = host.executionMode();
        boolean sshMode = ToolSettingsRepository.EXECUTION_SSH.equals(executionMode);
        boolean termuxSsh = sshMode && host.isTermuxSshHost();
        ArrayList<SheetOption> options = new ArrayList<>();
        ProjectRecord selected = projectStore.getSelectedProject(executionMode);
        String selectedId = selected == null ? "" : selected.getId();
        List<ProjectRecord> projects = projectStore.getProjects(executionMode);
        for (ProjectRecord project : projects) {
            options.add(new SheetOption(
                    "project:select:" + project.getId(),
                    project.getLabel(),
                    projectDisplayDescription(project),
                    project.getId().equals(selectedId),
                    projectDeleteActionId(project),
                    "Удалить"
            ));
        }
        if (!sshMode || termuxSsh) {
            options.add(new SheetOption(
                    "project:open_local_saf",
                    "Открыть локальный проект",
                    termuxSsh ? "Выбрать папку на телефоне как путь SSH-проекта" : "Выбрать папку через SAF и сохранить как локальный проект",
                    false
            ));
        }
        options.add(new SheetOption(
                "project:create",
                "Создать рабочую область",
                sshMode ? "Создать проект в SSH ~/.linecode/project" : "Создать проект в .linecode/project",
                false
        ));
        if (!sshMode || termuxSsh) {
            options.add(new SheetOption(
                    "storage:manage_all_files",
                    "Доступ ко всем файлам",
                    host.hasExternalStorageAccess() ? "Разрешено, хранилище доступно" : host.storagePermissionMessage(),
                    host.hasExternalStorageAccess()
            ));
        }
        return new ProjectSheet(sshMode ? "Рабочая область SSH" : "Рабочая область", options);
    }

    private String projectDeleteActionId(ProjectRecord project) {
        if (project == null
                || WorkspacePaths.DEFAULT_PROJECT_ID.equals(project.getId())
                || "ssh:default".equals(project.getId())) {
            return "";
        }
        return "project:delete:" + project.getId();
    }

    private String projectDisplayDescription(ProjectRecord project) {
        if (project == null) {
            return "";
        }
        String path = WorkspacePaths.displayPath(project.getPath());
        if (WorkspacePaths.SOURCE_SSH.equals(project.getSource()) && path.length() == 0) {
            return "Каталог входа SSH";
        }
        if (path.length() > 0) {
            return path;
        }
        return project.getDescription();
    }
}
