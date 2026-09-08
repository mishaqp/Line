package cn.lineai.ui.component;

import android.content.Context;
import android.view.ViewParent;
import android.widget.FrameLayout;
import cn.lineai.model.ExtensionAgentConfig;
import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.model.McpToolConfig;
import cn.lineai.navigation.LineDestination;
import cn.lineai.tool.BaseTool;
import cn.lineai.ui.MainChatView;
import java.util.Collections;
import java.util.List;

/** Legacy ScreenRegistry boundary. The editor UI/state live in Foundation v2. */
public final class AgentExtensionEditScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        ExtensionAgentConfig onGenerateDraft(String description) throws Exception;

        void onSave(ExtensionAgentConfig config);
    }

    private final LineDestination.AgentEdit destination;
    private final AgentExtensionEditorHostView hostView;

    public AgentExtensionEditScreenView(
            Context context,
            ExtensionAgentConfig editingAgent,
            List<BaseTool> availableTools,
            List<McpToolConfig> builtInMcps,
            List<ExtensionMcpConfig> customMcps,
            Listener listener
    ) {
        super(context);
        destination = new LineDestination.AgentEdit(editingAgent == null ? null : editingAgent.getId());

        AgentExtensionEditorLegacyGateway gateway = new AgentExtensionEditorLegacyGateway() {
            @Override
            public ExtensionAgentConfig editingAgent() {
                return editingAgent;
            }

            @Override
            public List<BaseTool> availableTools() {
                return availableTools == null ? Collections.emptyList() : availableTools;
            }

            @Override
            public List<McpToolConfig> builtInMcps() {
                return builtInMcps == null ? Collections.emptyList() : builtInMcps;
            }

            @Override
            public List<ExtensionMcpConfig> customMcps() {
                return customMcps == null ? Collections.emptyList() : customMcps;
            }

            @Override
            public ExtensionAgentConfig generateAgentDraft(String description) throws Exception {
                return listener.onGenerateDraft(description);
            }

            @Override
            public void saveAgentExtension(ExtensionAgentConfig config) {
                MainChatView owner = findMainChatView();
                listener.onSave(config);
                if (owner != null) {
                    owner.evictScreen(destination.getScreenId());
                }
                disposeEditor();
            }
        };

        hostView = new AgentExtensionEditorHostView(
                context,
                destination,
                new AgentExtensionEditorControllerRepository(gateway),
                new AgentExtensionEditorHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }
                }
        );
        addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void disposeEditor() {
        hostView.disposeEditor();
    }

    private MainChatView findMainChatView() {
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof MainChatView) {
                return (MainChatView) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
}
