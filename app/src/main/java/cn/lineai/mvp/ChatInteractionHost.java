package cn.lineai.mvp;

import cn.lineai.ai.ModelCancellationToken;
import cn.lineai.model.InputAttachment;
import java.util.ArrayList;

class ChatInteractionHost implements ChatInteractionController.Host {
    private final MainCoordinator coordinator;

    ChatInteractionHost(MainCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public String nextId() {
        return coordinator.nextId();
    }

    @Override
    public String agentTerminatedMessage() {
        return coordinator.agentTerminatedMessage();
    }

    @Override
    public String syncModePermission() {
        return coordinator.syncModePermission();
    }

    @Override
    public String projectPath() {
        return coordinator.projectState().path();
    }

    @Override
    public void ensureCurrentConversation() {
        coordinator.ensureCurrentConversation();
    }

    @Override
    public void persistCurrentConversation() {
        coordinator.persistCurrentConversation();
    }

    @Override
    public void loadConversation(String id) {
        coordinator.loadConversation(id);
    }

    @Override
    public void cancelActiveGeneration() {
        coordinator.generationLifecycleController().cancelActiveGeneration();
    }

    @Override
    public void startGenerationKeepAlive() {
        coordinator.generationLifecycleController().startKeepAlive();
    }

    @Override
    public void stopGenerationKeepAlive() {
        coordinator.generationLifecycleController().stopKeepAlive();
    }

    @Override
    public void setCurrentCancellationToken(ModelCancellationToken cancellationToken) {
        coordinator.generationLifecycleController().setCurrentCancellationToken(cancellationToken);
    }

    @Override
    public void markStreamingMessagesStopped() {
        coordinator.generationLifecycleController().markStreamingMessagesStopped();
    }

    @Override
    public void resetTodoState() {
        coordinator.resetTodoState();
    }

    @Override
    public void hideOverlays() {
        coordinator.viewHideOverlays();
    }

    @Override
    public void showChatScreen() {
        coordinator.viewShowChatScreen();
    }

    @Override
    public void setComposerDraft(String text, ArrayList<InputAttachment> attachments) {
        coordinator.viewProxy().setComposerDraft(text, attachments);
    }

    @Override
    public void render() {
        coordinator.render();
    }
}
