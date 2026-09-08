package cn.lineai.ui.component;

import android.content.Context;
import android.widget.FrameLayout;

/**
 * Legacy constructor boundary for {@code toolcall_preview}.
 * The screen itself is hosted by {@link ToolCallPreviewHostView}.
 */
public final class ToolCallPreviewScreenView extends FrameLayout {

    public ToolCallPreviewScreenView(Context context, Runnable onBack) {
        super(context);
        ToolCallPreviewRegistryRepository adapter =
                new ToolCallPreviewRegistryRepository(new DefaultToolCallPreviewRegistrySource());
        ToolCallPreviewHostView host = new ToolCallPreviewHostView(
                context,
                adapter,
                adapter,
                new ToolCallPreviewHostView.Listener() {
                    @Override
                    public void onBack() {
                        if (onBack != null) {
                            onBack.run();
                        }
                    }
                }
        );
        addView(host, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }
}
