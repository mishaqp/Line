package cn.lineai.ui.component;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import cn.lineai.model.InputAttachment;
import cn.lineai.ui.model.UserMessageAttachmentSnapshot;
import java.util.List;

/** Legacy user-message boundary. Attachment rendering/state live in Foundation v2. */
final class UserMessageAttachmentList extends LinearLayout {
    private final UserMessageAttachmentHostView hostView;

    UserMessageAttachmentList(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.END);
        UserMessageAttachmentRepository repository =
                new UserMessageAttachmentRepository();
        hostView = new UserMessageAttachmentHostView(context, repository);
        addView(
                hostView,
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        );
        setVisibility(GONE);
    }

    void bind(List<InputAttachment> attachments) {
        boolean visible = hostView.bind(attachments);
        setVisibility(visible ? VISIBLE : GONE);
    }

    static UserMessageAttachmentSnapshot snapshotOf(
            List<InputAttachment> attachments
    ) {
        UserMessageAttachmentRepository repository =
                new UserMessageAttachmentRepository();
        repository.replaceAll(attachments);
        return repository.snapshot();
    }
}
