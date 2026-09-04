package cn.lineai.ui.markdown;

import android.content.Context;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Collections;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;

public final class MarkdownView extends LinearLayout {
    private final Parser parser;
    private final MarkdownRenderer renderer;
    private int maxContentWidthPx;
    private String lastMarkdown;
    private boolean plainMode;
    private boolean codeWrapEnabled;
    private MarkdownLinkHandler linkHandler;

    public MarkdownView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setClipToPadding(false);
        Iterable<Extension> extensions = Collections.singletonList(TablesExtension.create());
        parser = Parser.builder().extensions(extensions).build();
        renderer = new MarkdownRenderer(context);
    }

    /**
     * Caps how wide this view may measure, in pixels; {@code 0} removes the cap.
     *
     * <p>Lets a host use {@code WRAP_CONTENT} so the view hugs a short paragraph, while
     * still bounding the wide blocks - fenced code and GFM tables measure their content
     * unbounded and would otherwise force the whole column open.</p>
     */
    public void setMaxContentWidth(int px) {
        int value = Math.max(0, px);
        if (maxContentWidthPx == value) {
            return;
        }
        maxContentWidthPx = value;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (maxContentWidthPx > 0) {
            int mode = MeasureSpec.getMode(widthMeasureSpec);
            int size = MeasureSpec.getSize(widthMeasureSpec);
            if (mode == MeasureSpec.UNSPECIFIED || size > maxContentWidthPx) {
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(
                        maxContentWidthPx,
                        mode == MeasureSpec.EXACTLY ? MeasureSpec.EXACTLY : MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setCodeWrapEnabled(boolean enabled) {
        if (codeWrapEnabled == enabled) {
            return;
        }
        codeWrapEnabled = enabled;
        renderer.setCodeWrapEnabled(enabled);
        if (!plainMode) {
            rerender();
        }
    }

    public void setLinkHandler(MarkdownLinkHandler linkHandler) {
        if (this.linkHandler == linkHandler) {
            return;
        }
        this.linkHandler = linkHandler;
        renderer.setLinkHandler(linkHandler);
        if (!plainMode) {
            rerender();
        }
    }

    public void setMarkdown(String markdown) {
        String value = markdown == null ? "" : markdown;
        if (!plainMode && value.equals(lastMarkdown)) {
            return;
        }
        plainMode = false;
        lastMarkdown = value;
        renderValue(value);
    }

    /**
     * 以纯文本（等宽字体）渲染，跳过 Markdown 解析。
     * 用于错误消息、流中断、异常堆栈等不应被按 markup 解析的内容。
     */
    public void setPlainText(String plainText) {
        String value = plainText == null ? "" : plainText;
        plainMode = true;
        lastMarkdown = null;
        removeAllViews();
        if (value.length() == 0) {
            return;
        }
        TextView text = new TextView(getContext());
        text.setTypeface(Typeface.MONOSPACE);
        text.setText(value);
        text.setTextIsSelectable(true);
        addView(text, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void rerender() {
        if (lastMarkdown == null) {
            return;
        }
        renderValue(lastMarkdown);
    }

    private void renderValue(String value) {
        removeAllViews();
        if (value.trim().length() == 0) {
            return;
        }
        Node document = parser.parse(value);
        renderer.renderInto(this, document);
    }
}
