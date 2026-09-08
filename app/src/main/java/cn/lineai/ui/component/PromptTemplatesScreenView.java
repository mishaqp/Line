package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.model.PromptTemplateItem;
import cn.lineai.ui.model.PromptTemplateUi;
import cn.lineai.ui.model.PromptTemplatesRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compatibility wrapper around the Compose Prompt Templates screen.
 * The ScreenRegistry route and controller save/reset callbacks stay unchanged.
 */
public final class PromptTemplatesScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onPromptTemplateSaved(String id, String value);

        void onPromptTemplateReset(String id);
    }

    public PromptTemplatesScreenView(Context context, List<PromptTemplateItem> templates, Listener listener) {
        super(context);
        PromptTemplatesRepository repository = new ListenerPromptTemplatesRepository(templates, listener);
        addView(
                new PromptTemplatesHostView(context, repository, listener::onBack),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerPromptTemplatesRepository implements PromptTemplatesRepository {
        private List<PromptTemplateUi> snapshot;
        private final Listener listener;

        ListenerPromptTemplatesRepository(List<PromptTemplateItem> templates, Listener listener) {
            this.snapshot = copyOf(templates);
            this.listener = listener;
        }

        @Override
        public List<PromptTemplateUi> templates() {
            return snapshot;
        }

        @Override
        public void saveTemplate(String id, String value) {
            String safeId = id == null ? "" : id;
            String safeValue = value == null ? "" : value;
            ArrayList<PromptTemplateUi> next = new ArrayList<>(snapshot.size());
            for (PromptTemplateUi item : snapshot) {
                if (item.getId().equals(safeId)) {
                    next.add(item.copy(
                            item.getId(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getSourceLabel(),
                            item.getVariables(),
                            item.getDefaultText(),
                            safeValue,
                            !item.getDefaultText().equals(safeValue)
                    ));
                } else {
                    next.add(item);
                }
            }
            snapshot = Collections.unmodifiableList(next);
            listener.onPromptTemplateSaved(safeId, safeValue);
        }

        @Override
        public void resetTemplate(String id) {
            String safeId = id == null ? "" : id;
            ArrayList<PromptTemplateUi> next = new ArrayList<>(snapshot.size());
            for (PromptTemplateUi item : snapshot) {
                if (item.getId().equals(safeId)) {
                    next.add(item.copy(
                            item.getId(),
                            item.getTitle(),
                            item.getDescription(),
                            item.getSourceLabel(),
                            item.getVariables(),
                            item.getDefaultText(),
                            item.getDefaultText(),
                            false
                    ));
                } else {
                    next.add(item);
                }
            }
            snapshot = Collections.unmodifiableList(next);
            listener.onPromptTemplateReset(safeId);
        }

        private static List<PromptTemplateUi> copyOf(List<PromptTemplateItem> templates) {
            List<PromptTemplateItem> source = templates == null
                    ? Collections.emptyList()
                    : templates;
            ArrayList<PromptTemplateUi> items = new ArrayList<>(source.size());
            for (PromptTemplateItem item : source) {
                items.add(PromptTemplateUi.Companion.from(item));
            }
            return Collections.unmodifiableList(items);
        }
    }
}
