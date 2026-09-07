package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;
import cn.lineai.ui.model.DataSettingsRepository;

/**
 * Compatibility wrapper around the Compose Data archive screen.
 */
public final class DataSettingsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();

        void onExport();

        void onImport();
    }

    public DataSettingsScreenView(Context context, Listener listener) {
        super(context);
        DataSettingsRepository repository = new ListenerDataSettingsRepository(listener);
        addView(
                new DataSettingsHostView(context, repository, listener::onBack),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerDataSettingsRepository
            implements DataSettingsRepository {
        private final Listener listener;

        ListenerDataSettingsRepository(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void exportAll() {
            listener.onExport();
        }

        @Override
        public void importLineCode() {
            listener.onImport();
        }
    }
}
