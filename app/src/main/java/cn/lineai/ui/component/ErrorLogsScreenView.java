package cn.lineai.ui.component;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.FrameLayout;
import cn.lineai.R;
import cn.lineai.log.ErrorLogEntry;
import cn.lineai.log.ErrorLogFileProvider;
import cn.lineai.ui.model.ErrorLogItem;
import cn.lineai.ui.model.ErrorLogsRepository;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility wrapper around the Compose error-log screen.
 */
public final class ErrorLogsScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
        List<ErrorLogEntry> onLoadLogs();
        void onClearLogs();
    }

    public ErrorLogsScreenView(Context context, Listener listener) {
        super(context);
        ErrorLogsRepository repository = new ListenerErrorLogsRepository(context, listener);
        addView(
                new ErrorLogsHostView(context, repository, listener::onBack),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        );
    }

    private static final class ListenerErrorLogsRepository
            implements ErrorLogsRepository {
        private final Context context;
        private final Listener listener;

        ListenerErrorLogsRepository(Context context, Listener listener) {
            this.context = context;
            this.listener = listener;
        }

        @Override
        public List<ErrorLogItem> loadLogs() {
            List<ErrorLogEntry> entries = listener.onLoadLogs();
            List<ErrorLogItem> items = new ArrayList<>();
            if (entries == null) {
                return items;
            }
            for (ErrorLogEntry entry : entries) {
                items.add(new ErrorLogItem(
                        entry.getFile(),
                        entry.getTitle(),
                        entry.getSubtitle(),
                        entry.getTimestamp()
                ));
            }
            return items;
        }

        @Override
        public void clearLogs() {
            listener.onClearLogs();
        }

        @Override
        public boolean openLog(File file) {
            try {
                Uri uri = ErrorLogFileProvider.uriFor(context, file);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "text/plain");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(Intent.createChooser(
                        intent,
                        context.getString(R.string.screen_error_logs_open_with)
                ));
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
    }
}
