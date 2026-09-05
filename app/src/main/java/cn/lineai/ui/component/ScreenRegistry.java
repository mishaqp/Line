package cn.lineai.ui.component;

import android.content.Context;
import android.view.View;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelProtocolType;
import cn.lineai.mvp.MainUiController;
import cn.lineai.ui.MainChatView;
import cn.lineai.ui.model.AccountModelProvider;
import cn.lineai.ui.model.AccountModelProviders;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Routes a screen id to the {@link ScreenFactory} that builds it.
 *
 * <p>Account-backed model editors are handled by the shared Compose screen;
 * legacy Java factories remain the fallback for API-key, local and other
 * providers during the incremental migration.</p>
 */
public final class ScreenRegistry {
    private final Map<String, ScreenFactory> factories = new LinkedHashMap<>();

    public void register(String id, ScreenFactory factory) {
        if (id == null || factory == null) {
            return;
        }
        factories.put(id, factory);
    }

    public void register(ScreenFactory factory) {
        register(factory.screenId(), factory);
    }

    public View createScreen(String id, MainChatView view, MainUiController controller, Context context) {
        if ("grokAccount".equals(id)) {
            return new GrokAccountScreenView(
                    context,
                    view::handleScreenBack,
                    () -> controller.onSettingsItemSelected("modelAdd:preset:grok")
            );
        }

        if ("modelAdd:preset:codex".equals(id)) {
            return createAccountModelEditor(
                    context,
                    view,
                    controller,
                    AccountModelProviders.fromProtocol(ModelProtocolType.CODEX_RESPONSES),
                    null
            );
        }
        if ("modelAdd:preset:grok".equals(id)) {
            return createAccountModelEditor(
                    context,
                    view,
                    controller,
                    AccountModelProviders.fromProtocol(ModelProtocolType.GROK_RESPONSES),
                    null
            );
        }

        if (id != null && id.startsWith("modelEdit:")) {
            String modelId = id.substring("modelEdit:".length());
            ModelConfig model = controller.getModel(modelId);
            AccountModelProvider provider = model == null
                    ? null
                    : AccountModelProviders.fromProtocol(model.getProtocolType());
            if (provider != null) {
                return createAccountModelEditor(context, view, controller, provider, model);
            }
        }

        if (id != null) {
            ScreenFactory exact = factories.get(id);
            if (exact != null) {
                return exact.createScreen(view, controller, context);
            }
        }
        for (ScreenFactory factory : factories.values()) {
            if (factory.matches(id)) {
                return factory.createScreen(view, controller, context);
            }
        }
        return null;
    }

    private View createAccountModelEditor(
            Context context,
            MainChatView view,
            MainUiController controller,
            AccountModelProvider provider,
            ModelConfig editingModel
    ) {
        if (provider == null) {
            return null;
        }
        return new AccountModelEditorScreenView(
                context,
                provider,
                editingModel,
                new AccountModelEditorScreenView.Listener() {
                    @Override
                    public void onBack() {
                        view.handleScreenBack();
                    }

                    @Override
                    public void onSave(ModelConfig model) {
                        controller.onModelSaved(model);
                    }

                    @Override
                    public void onTest(ModelConfig model) {
                        controller.onModelTest(model);
                    }

                    @Override
                    public void onOpenAccount(String screenId) {
                        controller.onSettingsItemSelected(screenId);
                    }
                }
        );
    }
}
