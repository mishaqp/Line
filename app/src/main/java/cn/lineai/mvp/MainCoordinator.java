package cn.lineai.mvp;

import android.content.Context;
import cn.lineai.R;
import cn.lineai.ai.ModelClient;
import cn.lineai.ai.prompt.SystemPromptProvider;
import cn.lineai.context.ContextCompactionService;
import cn.lineai.context.ContextManager;

import cn.lineai.data.repository.AiBehaviorSettingsRepository;
import cn.lineai.model.AgentTaskRecord;
import cn.lineai.model.AgentTaskState;
import cn.lineai.mvp.agent.AgentRuntimeController;
import cn.lineai.data.repository.ChatModeRepository;
import cn.lineai.data.repository.ConversationRecord;
import cn.lineai.data.repository.ConversationStore;
import cn.lineai.data.repository.DiffStore;
import cn.lineai.data.repository.ExtensionStore;
import cn.lineai.data.repository.FileTreeStore;
import cn.lineai.data.repository.InputSettingsRepository;
import cn.lineai.data.repository.IpcFileTreeStore;
import cn.lineai.data.repository.IpcProviderStore;
import cn.lineai.data.repository.LearningContextStore;
import cn.lineai.data.repository.OutputSettingsRepository;
import cn.lineai.data.repository.ProjectRecord;
import cn.lineai.data.repository.ProjectStore;
import cn.lineai.data.repository.PromptTemplateRepository;
import cn.lineai.data.repository.SshFileTreeStore;
import cn.lineai.data.repository.ThemeSettingsRepository;
import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.data.repository.KeepAliveRepository;
import cn.lineai.data.repository.StorageStatsRepository;
import cn.lineai.ipc.IpcProviderConfig;
import cn.lineai.ipc.ScannedProvider;
import cn.lineai.mvp.agent.AgentExecutionController;
import cn.lineai.model.AiBehaviorSettings;
import cn.lineai.model.ConversationUiModel;
import cn.lineai.model.ExtensionAgentConfig;
import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.data.model.ExtensionOverviewState;
import cn.lineai.model.InputSettings;
import cn.lineai.model.McpRequestHeader;
import cn.lineai.model.McpSettingsState;
import cn.lineai.model.McpToolSummary;
import cn.lineai.model.MemoryOverviewState;
import cn.lineai.model.OutputSettings;
import cn.lineai.model.PromptTemplateItem;
import cn.lineai.model.SkillRecord;
import cn.lineai.model.ThemeSettingsState;
import cn.lineai.model.WebSearchConfig;
import cn.lineai.security.UrlPolicy;
import cn.lineai.tool.BaseTool;
import cn.lineai.model.ChatMessage;
import cn.lineai.model.FileTreeNode;
import cn.lineai.model.KeepAliveSettings;
import cn.lineai.model.StorageStatsUiModel;
import cn.lineai.model.InputAttachment;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelStore;
import cn.lineai.model.SshConfig;
import cn.lineai.ssh.SshService;
import cn.lineai.ssh.TermuxHelper;
import cn.lineai.service.KeepAliveService;
import cn.lineai.log.ErrorLogEntry;
import cn.lineai.tool.ToolExecutor;
import cn.lineai.tool.ToolExecutionCoordinator;
import cn.lineai.tool.ToolRegistry;
import cn.lineai.workspace.SafPathResolver;
import cn.lineai.workspace.StoragePermissionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MainCoordinator implements MainUiController {
    String agentTerminatedMessage() {
        return context.getString(R.string.message_agent_terminated);
    }

    private final Context context;

    private final ChatSessionStore chatSessionStore = new ChatSessionStore();
    private final ArrayList<ChatMessage> messages = chatSessionStore.mutableMessages();
    private final ScreenNavigationController screenNavigationController = new ScreenNavigationController();
    private final MainThreadDispatcher mainThread;
    private final BackgroundTaskRunner backgroundTasks;
    ChatUiStateAssembler chatUiStateAssembler;
    ToolMessageController toolMessageController;
    ToolReviewController toolReviewController;
    ConversationPersistenceController conversationPersistenceController;
    ExtensionDraftController extensionDraftController;
    ExtensionManagementController extensionManagementController;
    ModelPromptController modelPromptController;
    DirectoryPickerController directoryPickerController;
    StorageMaintenanceController storageMaintenanceController;
    private final PhoneControlController phoneControlController;
    private final ErrorLogController errorLogController;
    private final cn.lineai.data.repository.StorageStatsRepository storageStatsRepository;
    private final cn.lineai.data.repository.KeepAliveRepository keepAliveRepository;
    ContextCompactionController contextCompactionController;
    IpcProviderController ipcProviderController;
    final GenerationController generationController = new GenerationController();
    GenerationLifecycleController generationLifecycleController;
    GenerationFlowController generationFlowController;
    ChatInteractionController chatInteractionController;
    ModelInteractionController modelInteractionController;
    OverlayActionController overlayActionController;
    ModelManagementController modelManagementController;
    SettingsManagementController settingsManagementController;
    SshFileTreeController sshFileTreeController;
    IpcFileTreeController ipcFileTreeController;
    FileTreeInteractionController fileTreeInteractionController;
    FileOperationController fileOperationController;
    PermissionModeController permissionModeController;
    ProjectWorkspaceController projectWorkspaceController;
    private final ModelStore modelRepository;
    private final AiBehaviorSettingsRepository aiBehaviorSettingsRepository;
    private final ChatModeRepository chatModeRepository;
    private final InputSettingsRepository inputSettingsRepository;
    private final OutputSettingsRepository outputSettingsRepository;
    private final ThemeSettingsRepository themeSettingsRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final ConversationStore conversationRepository;
    private final ProjectStore projectRepository;
    private final LearningContextStore learningContextRepository;
    private final ToolSettingsStore toolSettingsRepository;
    private final ExtensionStore extensionRepository;
    private final IpcProviderStore ipcProviderRepository;
    private final cn.lineai.ipc.IpcProviderManager ipcProviderManager;
    private final DiffStore diffRepository;
    private final FileTreeStore fileTreeRepository;
    private final IpcFileTreeStore ipcFileTreeRepository;
    private final SshService sshService;
    private final SshFileTreeStore sshFileTreeRepository;
    private final ContextManager contextManager;
    private final ContextCompactionService contextCompactionService;
    private final ModelClient modelClient;
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;
    private final ToolExecutionCoordinator toolExecutionCoordinator;
    private final SystemPromptProvider systemPromptProvider;
    private final StoragePermissionManager storagePermissionManager;
    private final SafPathResolver safPathResolver;
    LineCodeArchiveController lineCodeArchiveController;
    AgentExecutionController agentExecutionController;
    final AgentRuntimeController agentRuntimeController;
    private final cn.lineai.state.TodoStateStore todoStateStore;
    private final ViewProxy viewProxy = new ViewProxy();
    private final ScreenNavigationController.Host navigationHost = new ScreenNavigationController.Host() {
        @Override
        public void hideOverlays() {
            viewProxy.hideOverlays();
        }

        @Override
        public void showScreen(String screenId) {
            viewProxy.showScreen(screenId);
        }

        @Override
        public void showScreen(String screenId, boolean forward) {
            viewProxy.showScreen(screenId, forward);
        }

        @Override
        public void showScreen(String screenId, boolean forward, boolean animate) {
            viewProxy.showScreen(screenId, forward, animate);
        }

        @Override
        public void showChatScreen() {
            viewProxy.showChatScreen();
        }
    };
    private final ProjectRuntimeState projectState = new ProjectRuntimeState();
    AttachmentPickerCoordinator attachmentPickerController;

    public MainCoordinator(Context context) {
        this(new MainDependencies(context));
    }

    public MainCoordinator(MainDependencies dependencies) {
        // === assignDependencies ===
        this.context = dependencies.context;
        modelRepository = dependencies.modelRepository;
        aiBehaviorSettingsRepository = dependencies.aiBehaviorSettingsRepository;
        chatModeRepository = dependencies.chatModeRepository;
        inputSettingsRepository = dependencies.inputSettingsRepository;
        outputSettingsRepository = dependencies.outputSettingsRepository;
        themeSettingsRepository = dependencies.themeSettingsRepository;
        promptTemplateRepository = dependencies.promptTemplateRepository;
        conversationRepository = dependencies.conversationRepository;
        projectRepository = dependencies.projectRepository;
        learningContextRepository = dependencies.learningContextRepository;
        toolSettingsRepository = dependencies.toolSettingsRepository;
        extensionRepository = dependencies.extensionRepository;
        ipcProviderRepository = dependencies.ipcProviderRepository;
        ipcProviderManager = dependencies.ipcProviderManager;
        diffRepository = dependencies.diffRepository;
        fileTreeRepository = dependencies.fileTreeRepository;
        ipcFileTreeRepository = dependencies.ipcFileTreeRepository;
        sshService = dependencies.sshService;
        sshFileTreeRepository = dependencies.sshFileTreeRepository;
        contextManager = dependencies.contextManager;
        contextCompactionService = dependencies.contextCompactionService;
        modelClient = dependencies.modelClient;
        toolRegistry = dependencies.toolRegistry;
        toolExecutor = dependencies.toolExecutor;
        toolExecutionCoordinator = dependencies.toolExecutionCoordinator;
        systemPromptProvider = dependencies.systemPromptProvider;
        storagePermissionManager = dependencies.storagePermissionManager;
        safPathResolver = dependencies.safPathResolver;
        mainThread = dependencies.mainThreadDispatcher;
        backgroundTasks = dependencies.backgroundTaskRunner;
        todoStateStore = dependencies.todoStateStore;
        storageStatsRepository = dependencies.storageStatsRepository;
        keepAliveRepository = dependencies.keepAliveRepository;
        phoneControlController = dependencies.phoneControlController;
        errorLogController = dependencies.errorLogController;

        agentRuntimeController = new AgentRuntimeController(dependencies.agentTaskRepository);
        agentRuntimeController.recoverInFlightTasks();

        // === initControllers ===
        MainControllerInitializer.init(this, dependencies);

        // === initStartup ===
        applyProject(projectRepository.ensureSelectedProjectPath(toolSettingsRepository.getExecutionMode()));
        fileTreeInteractionController.addExpandedPath(projectState.path());
        loadCurrentConversation();
    }

    @Override
    public void attachView(MainContract.View view) {
        viewProxy.attach(view);
        applyProject(projectRepository.ensureSelectedProjectPath(toolSettingsRepository.getExecutionMode()));
        fileTreeInteractionController.addExpandedPath(projectState.path());
        render();
        requestSshFileTreeLoad(false);
        requestIpcFileTreeLoad(false);
        projectWorkspaceController.validateSelectedProjectAvailabilityOnStartup();
        chatInteractionController.resumePendingTasksForConversation(
                chatSessionStore.getCurrentConversationId()
        );
    }

    @Override
    public void detachView() {
        viewProxy.detach();
    }

    @Override
    public void destroy() {
        ipcProviderManager.removeStateListener(ipcProviderController);
        detachView();
        agentRuntimeController.cancelActiveTask("Activity уничтожена.");
        generationLifecycleController.cancelActiveGeneration();
        generationLifecycleController.stopKeepAlive();
        backgroundTasks.shutdownNow();
        agentRuntimeController.shutdown();
    }

    public void resetGenerationState() {
        if (chatInteractionController != null) {
            chatInteractionController.stopGeneration();
            return;
        }
        generationLifecycleController.cancelActiveGeneration();
        chatSessionStore.setStreaming(false);
        chatSessionStore.invalidateActiveGeneration();
        generationLifecycleController.stopKeepAlive();
        if (viewProxy.isAttached()) {
            render();
        }
    }

    @Override
    public void onMenuClick() {
        requestSshFileTreeLoad(false);
        requestIpcFileTreeLoad(false);
        viewProxy.showDrawer();
    }

    @Override
    public void onScreenBack() {
        navigateScreenBack("");
    }

    @Override
    public void onScreenBackFrom(String screenId) {
        navigateScreenBack(screenId);
    }

    private void navigateScreenBack(String visibleScreenId) {
        screenNavigationController.backFrom(visibleScreenId, navigationHost);
    }
