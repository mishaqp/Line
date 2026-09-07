package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.R;
import cn.lineai.model.ChatUiState;

/** Legacy MainChatView boundary. The header UI and state live in Foundation v2. */
public final class HeaderView extends FrameLayout {
    public interface Listener {
        void onMenuClick();
        void onProjectClick();
        void onModeChanged(String mode);
        void onPermissionClick();
        void onNewConversationClick();
        void onMoreClick();
    }

    private final HeaderControllerRepository repository;
    private final HeaderHostView hostView;

    public HeaderView(Context context) {
        super(context);
        repository = new HeaderControllerRepository(
                context.getString(R.string.header_project_default)
        );
        hostView = new HeaderHostView(context, repository);
        addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    public void setListener(Listener listener) {
        repository.setListener(listener);
    }

    public void render(ChatUiState state) {
        hostView.render(repository.snapshot(state));
    }
}
