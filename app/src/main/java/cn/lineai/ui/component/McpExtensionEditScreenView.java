package cn.lineai.ui.component;

import android.content.Context;
import android.view.ViewParent;
import android.widget.FrameLayout;
import cn.lineai.model.ExtensionMcpConfig;
import cn.lineai.model.McpRequestHeader;
import cn.lineai.model.McpToolSummary;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.MainChatView;
import java.util.List;

/** Legacy ScreenRegistry boundary. The editor UI/state live in Foundation v2. */
public final class McpExtensionEditScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        List<McpToolSummary> onQueryTools(String url, List<McpRequestHeader> headers) throws Exception;

        void onSave(ExtensionMcpConfig config);
    }

    private final LineDestination.McpEdit destination;
    private final McpExtensionEditorHostView hostView;

    public McpExtensionEditScreenView(Context context, ExtensionMcpConfig editingMcp, Listener listener) {
        super(context);
        destination = new LineDestination.McpEdit(editingMcp == null ? null : editingMcp.getId());
        McpExtensionEditorLegacyGateway gateway = new McpExtensionEditorLegacyGateway() {
            @Override
            public ExtensionMcpConfig loadEditingMcp() {
                return editingMcp;
            }

            @Override
            public List<McpToolSummary> queryTools(String url, List<McpRequestHeader> headers) throws Exception {
                return listener.onQueryTools(url, headers);
            }

            @Override
            public void saveMcpExtension(ExtensionMcpConfig config) {
                MainChatView owner = findMainChatView();
                listener.onSave(config);
                if (owner != null) {
                    owner.evictScreen(destination.getScreenId());
                }
                disposeEditor();
            }
        };
        hostView = new McpExtensionEditorHostView(
                context,
                destination,
                new McpExtensionEditorControllerRepository(gateway),
                new McpExtensionEditorHostView.Listener() {
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
