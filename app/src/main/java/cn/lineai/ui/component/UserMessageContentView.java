package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.ChatMessage;
import cn.lineai.ui.model.UserMessageContentSnapshot;
import cn.lineai.ui.theme.LineTheme;

/** Legacy user-message boundary. Text bubble UI/state live in Foundation v2. */
final class UserMessageContentView extends FrameLayout {
    private final UserMessageContentHostView hostView;

    UserMessageContentView(
            Context context,
            String attachedFilesLabel
    ) {
        super(context);
        float density = context.getResources()
                .getDisplayMetrics()
                .density;
        int horizontalPaddingPx =
                LineTheme.dp(context, LineTheme.LG) * 2;
        int availableWidth = context.getResources()
                .getDisplayMetrics()
                .widthPixels - horizontalPaddingPx;
        float maxWidthDp =
                (availableWidth * 0.74f) / density;

        UserMessageContentRepository repository =
                new UserMessageContentRepository(
                        attachedFilesLabel,
                        maxWidthDp
                );
        hostView = new UserMessageContentHostView(
                context,
                repository
        );
        addView(
                hostView,
                new LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                )
        );
        setVisibility(GONE);
    }

    void bind(ChatMessage message) {
        boolean visible = hostView.bind(message);
        setVisibility(visible ? VISIBLE : GONE);
    }

    static UserMessageContentSnapshot snapshotOf(
            ChatMessage message,
            String attachedFilesLabel,
            float maxWidthDp
    ) {
        UserMessageContentRepository repository =
                new UserMessageContentRepository(
                        attachedFilesLabel,
                        maxWidthDp
                );
        repository.bind(message);
        return repository.snapshot();
    }
}
