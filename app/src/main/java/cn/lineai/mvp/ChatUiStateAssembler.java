package cn.lineai.mvp;

import cn.lineai.context.ContextManager;
import cn.lineai.context.ContextSnapshot;
import cn.lineai.data.repository.AiBehaviorSettingsRepository;
import cn.lineai.data.repository.InputSettingsRepository;
import cn.lineai.data.repository.OutputSettingsRepository;
import cn.lineai.data.repository.ToolSettingsStore;
import cn.lineai.model.AiBehaviorSettings;
import cn.lineai.model.ChatMessage;
import cn.lineai.model.ChatUiState;
import cn.lineai.model.InputSettings;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelContextInfo;
import cn.lineai.model.ModelContextParser;
import cn.lineai.model.ModelStore;
import cn.lineai.model.OutputSettings;
import cn.lineai.tool.ExecutionTargetLabel;
import cn.lineai.workspace.WorkspacePaths;
import java.util.List;

public final class ChatUiStateAssembler {
    private final ModelStore modelRepository;
    private final AiBehaviorSettingsRepository aiBehaviorSettingsRepository;
    private final InputSettingsRepository inputSettingsRepository;
    private final OutputSettingsRepository outputSettingsRepository;
    private final ContextManager contextManager;

    public ChatUiStateAssembler(
            ModelStore modelRepository,
            AiBehaviorSettingsRepository aiBehaviorSettingsRepository,
            InputSettingsRepository inputSettingsRepository,
            OutputSettingsRepository outputSettingsRepository,
            ContextManager contextManager
    ) {
        this.modelRepository = modelRepository;
        this.aiBehaviorSettingsRepository = aiBehaviorSettingsRepository;
        this.inputSettingsRepository = inputSettingsRepository;
        this.outputSettingsRepository = outputSettingsRepository;
        this.contextManager = contextManager;
    }

    public ChatUiState assemble(
            String projectLabel,
            String projectSource,
            String projectPath,
            String conversationId,
            String activeChatMode,
            boolean streaming,
            List<ChatMessage> messages
    ) {
        String mode = ExecutionTargetLabel.current();
        if (mode.length() == 0 && WorkspacePaths.SOURCE_SSH.equals(projectSource)) {
            mode = ToolSettingsStore.EXECUTION_SSH;
        }
        return assemble(projectLabel, projectSource, projectPath, conversationId, activeChatMode,
                streaming, messages, mode);
    }

    public ChatUiState assemble(
            String projectLabel,
            String projectSource,
            String projectPath,
            String conversationId,
            String activeChatMode,
            boolean streaming,
            List<ChatMessage> messages,
            String executionMode
    ) {
        ModelConfig selectedModel = modelRepository.getSelectedModel();
        boolean hasConfiguredModel = selectedModel != null;
        ModelContextInfo contextInfo = ModelContextParser.parse(selectedModel);
        AiBehaviorSettings aiSettings = aiBehaviorSettingsRepository.get();
        InputSettings inputSettings = inputSettingsRepository.get();
        OutputSettings outputSettings = outputSettingsRepository.get();
        ContextSnapshot contextSnapshot = contextManager.snapshot(messages, contextInfo.getContextTokens(),
                aiSettings.isPreserveReasoningEnabled());
        String modelLabel = selectedModel == null
                ? "Модель не выбрана"
                : contextInfo.getApiModelId();
        String selectedModelId = selectedModel == null ? "" : selectedModel.getId();
        List<ModelConfig> availableModels = modelRepository.getModels();
        String uiProjectPath = WorkspacePaths.SOURCE_SSH.equals(projectSource) && safe(projectPath).length() == 0
                ? "Каталог входа SSH"
                : safe(projectPath);
        String targetLabel = ExecutionTargetLabel.format(executionMode, uiProjectPath);
        return new ChatUiState(
                projectLabel,
                uiProjectPath,
                modelLabel,
                contextSnapshot.getPercent() + "% / " + contextInfo.getContextLabel(),
                contextSnapshot.getPercent(),
                streaming,
                hasConfiguredModel,
                aiSettings.isThinkingScrollEnabled(),
                aiSettings.isThinkingAutoExpandEnabled(),
                outputSettings.isCodeWrapEnabled(),
                outputSettings.getBrowserMode(),
                inputSettings.getEnterKeyBehavior(),
                activeChatMode,
                conversationId,
                messages,
                selectedModelId,
                availableModels,
                targetLabel
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
