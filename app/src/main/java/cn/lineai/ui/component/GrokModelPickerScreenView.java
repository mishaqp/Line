package cn.lineai.ui.component;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.lineai.R;
import cn.lineai.data.grok.GrokAuthManager;
import cn.lineai.data.grok.GrokModelsRepository;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelProtocolType;
import cn.lineai.ui.theme.IconButtonView;
import cn.lineai.ui.theme.LineTheme;
import java.util.List;

/** Small account-aware picker that never puts the OAuth token into ModelConfig. */
public final class GrokModelPickerScreenView extends ScreenScaffoldView {
    public interface Listener {
        void onBack();
        void onSave(ModelConfig model);
    }

    private final Context context;
    private final Listener listener;
    private final ModelConfig editingModel;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GrokModelPickerScreenView(Context context, ModelConfig editingModel, Listener listener) {
        super(context,
                context.getString(editingModel == null
                        ? R.string.screen_grok_model_picker_title
                        : R.string.screen_grok_model_picker_edit_title),
                listener::onBack,
                null);
        this.context = context;
        this.listener = listener;
        this.editingModel = editingModel;
        LineTheme.padding(getContent(), LineTheme.LG, LineTheme.LG, LineTheme.LG, 100);
        load();
    }

    private void load() {
        getContent().removeAllViews();
        TextView loading = LineTheme.text(context,
                context.getString(R.string.screen_grok_model_picker_loading),
                LineTheme.FONT_SM, LineTheme.TEXT_TERTIARY, android.graphics.Typeface.NORMAL);
        getContent().addView(loading, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        new Thread(() -> {
            try {
                List<String> models = GrokModelsRepository.fetchModelIds(context);
                mainHandler.post(() -> renderModels(models));
            } catch (Exception e) {
                mainHandler.post(this::renderError);
            }
        }, "linecode-grok-model-picker").start();
    }

    private void renderModels(List<String> models) {
        getContent().removeAllViews();
        if (models == null || models.isEmpty()) {
            renderError();
            return;
        }
        TextView hint = LineTheme.text(context,
                context.getString(R.string.screen_grok_model_picker_desc),
                LineTheme.FONT_SM, LineTheme.TEXT_SECONDARY, android.graphics.Typeface.NORMAL);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        hintParams.bottomMargin = LineTheme.dp(context, LineTheme.MD);
        getContent().addView(hint, hintParams);

        SettingsSectionView section = new SettingsSectionView(
                context, context.getString(R.string.screen_grok_models_section));
        for (int i = 0; i < models.size(); i++) {
            String modelId = models.get(i);
            boolean active = editingModel != null && modelId.equals(editingModel.getModelId());
            section.addRow(new OptionRowView(
                    context,
                    IconButtonView.SPARKLES,
                    modelId,
                    active ? context.getString(R.string.screen_grok_model_picker_current) : "",
                    active,
                    () -> save(modelId)
            ), i < models.size() - 1);
        }
        getContent().addView(section, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void renderError() {
        getContent().removeAllViews();
        TextView error = LineTheme.text(context,
                context.getString(R.string.screen_grok_model_picker_failed),
                LineTheme.FONT_SM, LineTheme.TEXT_SECONDARY, android.graphics.Typeface.NORMAL);
        getContent().addView(error, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void save(String modelId) {
        ModelConfig model;
        if (editingModel != null) {
            model = new ModelConfig(
                    editingModel.getId(),
                    editingModel.getName(),
                    ModelProtocolType.GROK_RESPONSES,
                    "Grok",
                    GrokAuthManager.API_BASE_URL,
                    "",
                    modelId,
                    editingModel.getToolCallLimit(),
                    editingModel.isCompressionModelEnabled(),
                    editingModel.isCompressionModelAuto(),
                    editingModel.getCompressionModelId(),
                    editingModel.getContextSize()
            );
        } else {
            model = ModelConfig.builder(
                            "",
                            modelId,
                            ModelProtocolType.GROK_RESPONSES,
                            "Grok",
                            GrokAuthManager.API_BASE_URL,
                            "",
                            modelId)
                    .build();
        }
        listener.onSave(model);
    }
}
