package cn.lineai.ui.component;

import android.content.Context;
import android.view.View;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelProviderPreset;
import cn.lineai.model.ModelProviderPresets;
import cn.lineai.navigation.LineDestination;
import cn.lineai.navigation.LineDestinations;
import cn.lineai.model.ModelProtocolType;
import cn.lineai.mvp.MainUiController;
import cn.lineai.ui.MainChatView;
import cn.lineai.ui.model.AccountModelProvider;
import cn.lineai.ui.model.AccountModelProviders;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes a screen id to the {@link ScreenFactory} that builds it.
 *
 * <p>Account-backed provider screens and model editors are handled by shared
 * Compose surfaces; legacy Java factories remain the fallback for API-key,
 * local and other providers during the incremental migration.</p>
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
        return createScreen(LineDestinations.fromScreenId(id), view, controller, context);
    }

    public View createScreen(
            LineDestination destination,
            MainChatView view,
            MainUiController controller,
            Context context
    ) {
        String id = destination == null ? "" : destination.getScreenId();
        if ("models".equals(id) || "modelAddOptions".equals(id)) {
            return createModelNavigationHost(context, view, controller, destination);
        }
        if ("codexAccount".equals(id)) {
            return createAccountScreen(
                    context,
                    view,
                    controller,
                    AccountModelProviders.fromProtocol(ModelProtocolType.CODEX_RESPONSES),
                    "codexAccount"
            );
        }
        if ("grokAccount".equals(id)) {
            return createAccountScreen(
                    context,
                    view,
                    controller,
                    AccountModelProviders.fromProtocol(ModelProtocolType.GROK_RESPONSES),
                    "grokAccount"
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

    private View createModelNavigationHost(
            Context context,
            MainChatView view,
            MainUiController controller,
            LineDestination startDestination
    ) {
        return new ModelNavigationHostView(
                context,
                controller.getModels(),
                controller.getSelectedModelId(),
                startDestination,
                new ModelNavigationHostView.Listener() {
                    @Override
                    public void onExit() {
                        view.handleScreenBack();
                    }

                    @Override
                    public void onSelectModel(String id) {
                        controller.onModelSelected(id);
                    }

                    @Override
                    public void onDeleteModels(List<String> ids) {
                        controller.onModelsDeleted(ids);
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
                    public ModelConfig getModel(String id) {
                        return controller.getModel(id);
                    }

                    @Override
                    public List<ModelConfig> models() {
                        return controller.getModels();
                    }

                    @Override
                    public String selectedModelId() {
                        return controller.getSelectedModelId();
                    }

                    @Override
                    public View createLegacyEditor(
                            Context editorContext,
                            LineDestination destination,
                            Runnable onBack
                    ) {
                        String id = destination.getScreenId();
                        ModelProviderPreset preset = null;
                        boolean local = "modelAdd:local".equals(id);
                        ModelConfig editingModel = null;

                        if (id.startsWith("modelAdd:preset:")) {
                            preset = ModelProviderPresets.find(
                                    id.substring("modelAdd:preset:".length())
                            );
                        } else if (id.startsWith("modelEdit:")) {
                            editingModel = controller.getModel(
                                    id.substring("modelEdit:".length())
                            );
                            local = editingModel != null
                                    && editingModel.getProtocolType() == ModelProtocolType.LOCAL_GGUF;
                        }

                        return ScreenFactories.newModelAddScreen(
                                editorContext,
                                controller,
                                preset,
                                local,
                                editingModel,
                                onBack
                        );
                    }
                }
        );
    }

    private View createAccountScreen(
            Context context,
            MainChatView view,
            MainUiController controller,
            AccountModelProvider provider,
            String startScreenId
    ) {
        if (provider == null) {
            return null;
        }
        return new AccountNavigationHostView(
                context,
                provider,
                null,
                LineDestinations.fromScreenId(startScreenId),
                new AccountNavigationHostView.Listener() {
                    @Override
                    public void onExit() {
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
                }
        );
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
        String providerId = provider.getProtocolType() == ModelProtocolType.CODEX_RESPONSES
                ? "codex"
                : "grok";
        String startScreenId = editingModel == null
                ? "modelAdd:preset:" + providerId
                : "modelEdit:" + editingModel.getId();

        return new AccountNavigationHostView(
                context,
                provider,
                editingModel,
                LineDestinations.fromScreenId(startScreenId),
                new AccountNavigationHostView.Listener() {
                    @Override
                    public void onExit() {
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
                }
        );
    }

}
