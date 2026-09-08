package cn.lineai.ui.component;

import android.content.Context;
import android.view.ViewParent;
import android.widget.FrameLayout;
import cn.lineai.R;
import cn.lineai.data.codex.CodexAuthManager;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelProtocolType;
import cn.lineai.model.ModelProviderPreset;
import cn.lineai.navigation.LineDestination;
import cn.lineai.ui.MainChatView;
import cn.lineai.ui.theme.LineTheme;
import cn.lineai.ui.util.KeyboardController;
import cn.lineai.ui.util.ModelProviderPresetStrings;
import java.util.List;

/** Legacy ScreenRegistry boundary. The editor UI/state live in Foundation v2. */
public final class ModelAddScreenView extends FrameLayout {
    public interface Listener {
        void onBack();
        void onSave(ModelConfig model);
        void onTest(ModelConfig model);
        List<String> onFetchModelCatalog(ModelProtocolType type, String baseUrl, String apiKey) throws Exception;
    }

    private final LineDestination destination;
    private final ModelEditorHostView hostView;

    public ModelAddScreenView(Context context, ModelProviderPreset preset, boolean local, Listener listener) {
        this(context, preset, local, null, listener);
    }

    public ModelAddScreenView(
            Context context,
            ModelProviderPreset preset,
            boolean local,
            ModelConfig editingModel,
            Listener listener
    ) {
        super(context);
        setBackgroundColor(LineTheme.BG);
        this.destination = resolveDestination(preset, local, editingModel);

        ModelEditorLegacyGateway gateway = new ModelEditorLegacyGateway() {
            @Override
            public ModelProviderPreset preset() {
                return preset;
            }

            @Override
            public boolean localRequested() {
                return local;
            }

            @Override
            public ModelConfig editingModel() {
                return editingModel;
            }

            @Override
            public String presetLabel() {
                return preset == null ? null : ModelProviderPresetStrings.getLabel(context, preset.getId());
            }

            @Override
            public String presetHint() {
                return preset == null ? "" : ModelProviderPresetStrings.getHint(context, preset.getId());
            }

            @Override
            public String localProviderLabel() {
                return context.getString(R.string.model_provider_local);
            }

            @Override
            public String customProviderLabelSentinel() {
                return context.getString(R.string.model_provider_preset_custom_label);
            }

            @Override
            public boolean isCodexAuthenticated() {
                return CodexAuthManager.isAuthenticated(context);
            }

            @Override
            public List<String> fetchModelCatalog(ModelProtocolType type, String baseUrl, String apiKey)
                    throws Exception {
                return listener.onFetchModelCatalog(type, baseUrl, apiKey);
            }

            @Override
            public void saveModel(ModelConfig config) {
                MainChatView owner = findMainChatView();
                listener.onSave(config);
                if (owner != null) {
                    owner.evictScreen(destination.getScreenId());
                }
                disposeEditor();
            }

            @Override
            public void testModel(ModelConfig config) {
                listener.onTest(config);
            }
        };

        hostView = new ModelEditorHostView(
                context,
                destination,
                new ModelEditorControllerRepository(gateway),
                new ModelEditorHostView.Listener() {
                    @Override
                    public void onBack() {
                        listener.onBack();
                    }
                }
        );
        addView(hostView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void disposeEditor() {
        hostView.disposeEditor();
    }

    @Override
    protected void onDetachedFromWindow() {
        KeyboardController.clearFocusAndHide(this);
        super.onDetachedFromWindow();
    }

    private MainChatView findMainChatView() {
        ViewParent parent = getParent();
        while (parent != null) {
            if (parent instanceof MainChatView) {
                return (MainChatView) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    static LineDestination resolveDestination(
            ModelProviderPreset preset,
            boolean local,
            ModelConfig editingModel
    ) {
        if (editingModel != null) {
            return new LineDestination.ModelEdit(editingModel.getId());
        }
        if (local) {
            return LineDestination.ModelAddLocal.INSTANCE;
        }
        if (preset != null) {
            return new LineDestination.ModelAddPreset(preset.getId());
        }
        return LineDestination.ModelAdd.INSTANCE;
    }
}
