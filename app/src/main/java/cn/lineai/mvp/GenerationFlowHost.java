package cn.lineai.mvp;

import cn.lineai.R;
import cn.lineai.ai.ModelCancellationToken;
import cn.lineai.util.StringUtils;

class GenerationFlowHost implements GenerationFlowController.Host {
    private final MainCoordinator coordinator;

    GenerationFlowHost(MainCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public String nextId() {
        return coordinator.nextId();
    }

    @Override
    public String projectPath() {
        return coordinator.projectState().path();
    }

    @Override
    public String projectSource() {
        return coordinator.projectState().source();
    }

    @Override
    public String currentConversationId() {
        return coordinator.chatSessionStore().getCurrentConversationId();
    }

    @Override
    public String syncModePermission() {
        return coordinator.syncModePermission();
    }

    @Override
    public void persistCurrentConversation() {
        coordinator.persistCurrentConversation();
    }

    @Override
    public void render() {
        coordinator.render();
    }

    @Override
    public void stopGenerationKeepAlive() {
        coordinator.generationLifecycleController.stopKeepAlive();
    }

    @Override
    public void setCurrentCancellationToken(ModelCancellationToken cancellationToken) {
        coordinator.generationLifecycleController.setCurrentCancellationToken(cancellationToken);
    }

    @Override
    public boolean isSshExecutionMode() {
        return coordinator.isSshExecutionMode();
    }

    @Override
    public boolean isTerminalProviderExecutionMode() {
        return coordinator.isTerminalProviderExecutionMode();
    }

    @Override
    public String formatRetryNotice(int attempt, int maxRetries, String error) {
        return coordinator.context().getString(
                R.string.model_retry_attempt,
                attempt,
                maxRetries,
                StringUtils.decodeUnicodeEscapes(error)
        );
    }

    @Override
    public String formatModelFailed(String error) {
        return coordinator.context().getString(
                R.string.model_retry_failed,
                StringUtils.decodeUnicodeEscapes(error)
        );
    }

    @Override
    public String toolLimitNotExecutedMessage() {
        return coordinator.context().getString(R.string.tool_call_limit_not_executed);
    }

    @Override
    public void onGenerationCheckpoint(int generationId, String phase, int toolCallCount, String payloadJson) {
        coordinator.onGenerationCheckpoint(generationId, phase, toolCallCount, payloadJson);
    }

    @Override
    public void onGenerationFinished(int generationId, boolean success, String error) {
        coordinator.onGenerationFinished(generationId, success, error);
    }

    @Override
    public boolean isGenerationDeadlineExceeded(int generationId) {
        return coordinator.isGenerationDeadlineExceeded(generationId);
    }

    @Override
    public String generationBudgetExceededMessage() {
        return coordinator.generationBudgetExceededMessage();
    }
}
