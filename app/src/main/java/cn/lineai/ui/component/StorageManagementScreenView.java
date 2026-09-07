package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.StorageStatsUiModel;
import cn.lineai.ui.model.StorageManagementRepository;

public final class StorageManagementScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
        void onClearDiffCache();
        void onClearChatHistory();
        StorageStatsUiModel onLoadStats();
    }

    private final StorageManagementHostView hostView;

    public StorageManagementScreenView(Context context, Listener listener) {
        super(context);
        StorageManagementRepository repository = new StorageManagementRepository() {
            @Override
            public StorageStatsUiModel loadStats() {
                return listener.onLoadStats();
            }
        };
        hostView = new StorageManagementHostView(context, repository, new StorageManagementHostView.Listener() {
            @Override
            public void onBack() {
                listener.onBack();
            }

            @Override
            public void onClearDiffCache() {
                listener.onClearDiffCache();
            }

            @Override
            public void onClearChatHistory() {
                listener.onClearChatHistory();
            }
        });
        addView(
                hostView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    public void refresh() {
        hostView.refresh();
    }
}
