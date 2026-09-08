package cn.lineai.ui.component;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

import cn.lineai.R;
import cn.lineai.model.ConversationUiModel;
import cn.lineai.model.FileTreeNode;

import java.util.List;

/**
 * Thin legacy boundary for the Foundation v2 drawer.
 *
 * MainChatView keeps the existing Java API while state, rendering and user actions are owned by
 * DrawerHostView and DrawerViewModel.
 */
public final class DrawerView extends FrameLayout {
    public interface Listener {
        void onCloseDrawer();

        void onNewConversation();

        void onConversationSelected(String id);

        void onConversationDeleted(String id);

        void onCurrentProjectRemoveRequested();

        void onFileNodeSelected(String path, boolean directory);

        void onFileNodeLongPressed(String path, String name, boolean directory, boolean root);

        void onFileTreeActivated();

        void onFileTreeRefresh();
    }

    private final DrawerControllerRepository repository;
    private final DrawerHostView hostView;
    private Listener listener;

    public DrawerView(Context context) {
        super(context);
        setVisibility(GONE);
        setClickable(true);

        repository = new DrawerControllerRepository();
        hostView = new DrawerHostView(context, repository, new DrawerHostView.Listener() {
            @Override
            public void onClosed() {
                setVisibility(GONE);
                if (listener != null) {
                    listener.onCloseDrawer();
                }
            }

            @Override
            public void onNewConversation() {
                if (listener != null) {
                    listener.onNewConversation();
                }
            }

            @Override
            public void onConversationSelected(String id) {
                if (listener != null) {
                    listener.onConversationSelected(id);
                }
            }

            @Override
            public void onConversationDeleted(String id) {
                if (listener != null) {
                    listener.onConversationDeleted(id);
                }
            }

            @Override
            public void onCurrentProjectRemoveRequested() {
                if (listener != null) {
                    listener.onCurrentProjectRemoveRequested();
                }
            }

            @Override
            public void onFileNodeSelected(String path, boolean directory) {
                if (listener != null) {
                    listener.onFileNodeSelected(path, directory);
                }
            }

            @Override
            public void onFileNodeLongPressed(
                    String path,
                    String name,
                    boolean directory,
                    boolean root
            ) {
                if (listener != null) {
                    listener.onFileNodeLongPressed(path, name, directory, root);
                }
            }

            @Override
            public void onFileTreeActivated() {
                if (listener != null) {
                    listener.onFileTreeActivated();
                }
            }

            @Override
            public void onFileTreeRefresh() {
                if (listener != null) {
                    listener.onFileTreeRefresh();
                }
            }
        });
        addView(
                hostView,
                new FrameLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                )
        );
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void render(
            List<ConversationUiModel> conversations,
            String currentConversationId,
            String projectLabel,
            String projectPath,
            boolean projectRemovable,
            FileTreeNode fileTree
    ) {
        String resolvedProjectLabel = projectLabel == null || projectLabel.length() == 0
                ? getContext().getString(R.string.header_project_default)
                : projectLabel;
        repository.replace(
                conversations,
                currentConversationId,
                resolvedProjectLabel,
                projectPath,
                projectRemovable,
                fileTree
        );
        hostView.reload();
    }

    public void open() {
        if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }
        bringToFront();
        hostView.open();
    }

    public void close() {
        if (getVisibility() != View.VISIBLE) {
            return;
        }
        hostView.close();
    }

    public boolean isFilesTabActive() {
        return hostView.isFilesTabActive();
    }
}
