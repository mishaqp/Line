package cn.lineai.mvp;

import cn.lineai.data.lip.LipInstaller;
import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.data.repository.IpcProviderStore;
import cn.lineai.ipc.IpcProviderType;
import cn.lineai.model.ExtensionAgentConfig;
import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.data.model.ExtensionOverviewState;
import cn.lineai.model.McpRequestHeader;
import cn.lineai.model.McpToolSummary;
import cn.lineai.model.SkillRecord;
import cn.lineai.tool.ToolRegistry;
import java.io.File;
import java.util.List;

final class ExtensionManagementController {
    interface Host {
        String projectPath();

        void returnToScreen(String screenId);

        void refreshVisibleScreen(String screenId);

        void render();

        void showSkillError(String message);
    }

    private final ExtensionStore extensionRepository;
    private final IpcProviderStore ipcProviderRepository;
    private final ToolRegistry toolRegistry;
    private final LipInstaller lipInstaller;
    private final cn.lineai.data.service.SkillFileManager skillFileManager;
    private final BackgroundTaskRunner backgroundTasks;
    private final MainThreadDispatcher mainThread;
    private final Host host;

    ExtensionManagementController(
            ExtensionStore extensionRepository,
            IpcProviderStore ipcProviderRepository,
            ToolRegistry toolRegistry,
            LipInstaller lipInstaller,
            cn.lineai.data.service.SkillFileManager skillFileManager,
            BackgroundTaskRunner backgroundTasks,
            MainThreadDispatcher mainThread,
            Host host
    ) {
        this.extensionRepository = extensionRepository;
        this.ipcProviderRepository = ipcProviderRepository;
        this.toolRegistry = toolRegistry;
        this.lipInstaller = lipInstaller;
        this.skillFileManager = skillFileManager;
        this.backgroundTasks = backgroundTasks;
        this.mainThread = mainThread;
        this.host = host;
    }

    ExtensionOverviewState getOverview() {
        ExtensionOverviewState base = extensionRepository.getOverview(host.projectPath());
        return new ExtensionOverviewState(
                base.getAgents(),
                base.getMcps(),
                base.getSkills(),
                ipcProviderRepository.getProvidersByType(IpcProviderType.TERMINAL),
                lipInstaller == null ? null : lipInstaller.list()
        );
    }

    void saveAgentExtension(ExtensionAgentConfig config) {
        extensionRepository.saveAgentExtension(config);
        reloadExtensions();
        host.returnToScreen("extension:agent");
        host.render();
    }

    void saveMcpExtension(ExtensionMcpConfig config) {
        extensionRepository.saveMcpExtension(config);
        reloadExtensions();
        host.returnToScreen("extension:mcp");
        host.render();
    }

    List<McpToolSummary> queryMcpTools(String url, List<McpRequestHeader> headers) throws Exception {
        return extensionRepository.queryMcpTools(url, headers);
    }

    void createSkill(String location, String name, String description, String content) {
        backgroundTasks.execute("skill-create", () -> {
            try {
                extensionRepository.createSkill(host.projectPath(), location, name, description, content);
                mainThread.dispatch(this::completeSkillInstall);
            } catch (Exception e) {
                mainThread.dispatch(() -> host.showSkillError(errorMessage(e)));
            }
        });
    }

    void installSkill(String location, String sourcePath, String name) {
        backgroundTasks.execute("skill-install", () -> {
            try {
                extensionRepository.installSkill(host.projectPath(), location, sourcePath, name);
                mainThread.dispatch(this::completeSkillInstall);
            } catch (Exception e) {
                mainThread.dispatch(() -> host.showSkillError(errorMessage(e)));
            }
        });
    }

    void installSkillFromUri(String location, String uri, String displayName) {
        backgroundTasks.execute("skill-install-from-uri", () -> {
            try {
                extensionRepository.installSkillFromUri(host.projectPath(), location, uri, displayName);
                mainThread.dispatch(this::completeSkillInstall);
            } catch (Exception e) {
                mainThread.dispatch(() -> host.showSkillError(errorMessage(e)));
            }
        });
    }

    void installSkillFromGitHub(String location, String githubUrl) {
        backgroundTasks.execute("skill-install-from-github", () -> {
            try {
                extensionRepository.installSkillFromGitHub(host.projectPath(), location, githubUrl);
                mainThread.dispatch(this::completeSkillInstall);
            } catch (Exception e) {
                mainThread.dispatch(() -> host.showSkillError(errorMessage(e)));
            }
        });
    }

    void installLip(String location, String sourcePath) {
        backgroundTasks.execute("lip-install", () -> {
            try {
                lipInstaller.installFile(host.projectPath(), location, new File(sourcePath));
                mainThread.dispatch(this::completeLipInstall);
            } catch (Exception e) {
                mainThread.dispatch(() -> host.showSkillError(errorMessage(e)));
            }
        });
    }

    void installLipFromUri(String location, String uri, String displayName) {
        backgroundTasks.execute("lip-install-from-uri", () -> {
            File temp = null;
            try {
                String name = displayName == null || displayName.trim().length() == 0
                        ? "package.lip"
                        : displayName;
                File tempRoot = new File(skillFileManager.getWorkspacePaths().getLinecodeRoot(), "tmp/lip");
                tempRoot.mkdirs();
                temp = new File(tempRoot, System.currentTimeMillis() + "-" + name);
                skillFileManager.copyUriToFile(uri, temp);
                lipInstaller.installFile(host.projectPath(), location, temp);
                mainThread.dispatch(this::completeLipInstall);
            } catch (Exception e) {
                mainThread.dispatch(() -> host.showSkillError(errorMessage(e)));
            } finally {
                if (temp != null && temp.exists()) {
                    temp.delete();
                }
            }
        });
    }

    private void completeSkillInstall() {
        host.returnToScreen("extension:skills");
        host.render();
    }

    private void completeLipInstall() {
        reloadExtensions();
        host.returnToScreen("extension:linecode");
        host.render();
    }

    private String errorMessage(Exception e) {
        return e == null ? null : e.getMessage();
    }

    void deleteExtensions(String kind, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        if ("skills".equals(kind)) {
            extensionRepository.deleteSkills(ids);
        } else {
            for (String id : ids) {
                deleteExtension(kind, id);
            }
            return;
        }
        reloadExtensions();
        host.refreshVisibleScreen("extension:" + kind);
        host.render();
    }

    void setExtensionEnabled(String kind, String id, boolean enabled) {
        if ("linecode".equals(kind) && lipInstaller != null) {
            lipInstaller.setEnabled(id, enabled);
        } else {
            ExtensionKindDescriptor descriptor = ExtensionKindRegistry.getInstance().get(kind);
            if (descriptor != null) {
                descriptor.setEnabled(extensionRepository, id, enabled);
            }
        }
        reloadExtensions();
        host.refreshVisibleScreen("extension:" + kind);
        host.render();
    }

    void deleteExtension(String kind, String id) {
        if ("linecode".equals(kind) && lipInstaller != null) {
            lipInstaller.delete(id);
        } else {
            ExtensionKindDescriptor descriptor = ExtensionKindRegistry.getInstance().get(kind);
            if (descriptor != null) {
                descriptor.delete(extensionRepository, id);
            }
        }
        reloadExtensions();
        host.refreshVisibleScreen("extension:" + kind);
        host.render();
    }

    private void reloadExtensions() {
        toolRegistry.reloadExtensions();
    }
}
