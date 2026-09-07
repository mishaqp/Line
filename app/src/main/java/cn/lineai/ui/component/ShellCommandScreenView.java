package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * Legacy constructor boundary for {@code shellCommand}.
 * The screen itself is hosted by {@link ShellCommandHostView}.
 */
public final class ShellCommandScreenView extends FrameLayout {

    public interface Listener {
        void onBack();
    }

    public ShellCommandScreenView(Context context, String command, Listener listener) {
        this(context, new FixedShellCommandSource(command), listener);
    }

    public ShellCommandScreenView(Context context, ShellCommandSource source, Listener listener) {
        super(context);
        ShellCommandControllerRepository repository = new ShellCommandControllerRepository(source);
        ShellCommandHostView host = new ShellCommandHostView(
                context,
                repository,
                new ShellCommandHostView.Listener() {
                    @Override
                    public void onBack() {
                        if (listener != null) {
                            listener.onBack();
                        }
                    }
                }
        );
        addView(host, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private static final class FixedShellCommandSource implements ShellCommandSource {
        private final String command;

        private FixedShellCommandSource(String command) {
            this.command = command;
        }

        @Override
        public String currentCommand() {
            return command;
        }
    }
}
