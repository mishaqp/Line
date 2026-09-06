package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.MemoryOverviewState;
import cn.lineai.ui.model.MemorySettingsRepository;
import java.util.List;

public final class MemorySettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        MemoryOverviewState getMemoryOverview();

        void onMemorySaved(String id, String scope, String content);

        void onMemoryDeleted(String id);

        void onMemoriesDeleted(List<String> ids);
    }

    private final MemorySettingsHostView hostView;
    private boolean attachedOnce;

    public MemorySettingsScreenView(Context context, Listener listener) {
        super(context);
        MemorySettingsRepository repository = new MemorySettingsRepository() {
            @Override
            public MemoryOverviewState getMemoryOverview() {
                return listener.getMemoryOverview();
            }

            @Override
            public void onMemorySaved(String id, String scope, String content) {
                listener.onMemorySaved(id, scope, content);
            }

            @Override
            public void onMemoryDeleted(String id) {
                listener.onMemoryDeleted(id);
            }

            @Override
            public void onMemoriesDeleted(List<String> ids) {
                listener.onMemoriesDeleted(ids);
            }
        };
        hostView = new MemorySettingsHostView(
                context,
                repository,
                new MemorySettingsHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }
                }
        );
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (attachedOnce) {
            hostView.refresh();
            return;
        }
        attachedOnce = true;
    }
}
