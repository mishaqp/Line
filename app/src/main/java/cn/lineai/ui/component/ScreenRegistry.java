package cn.lineai.ui.component;

import android.content.Context;
import android.view.View;
import cn.lineai.model.ModelConfig;
import cn.lineai.model.ModelProtocolType;
import cn.lineai.mvp.MainUiController;
import cn.lineai.ui.MainChatView;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Routes a screen id to the {@link ScreenFactory} that builds it.
 *
 * <p>Lookup first handles the account-aware Grok screens, then tries an exact
 * match against the registered screen ids, then prefix-style factories.</p>
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
        if ("modelAdd:preset:grok".equals(id)) {
            return new GrokModelPickerScreenView(
                    context,
                    null,
                    new GrokModelPickerScreenView.Listener() {
                        @Override
                        public void onBack() {
                            view.handleScreenBack();
                        }

                        @Override
                        public void onSave(ModelConfig model) {
                            controller.onModelSaved(model);
                        }
                    }
            );
        }
        if (id != null && id.startsWith("modelEdit:")) {
            String modelId = id.substring("modelEdit:".length());
            ModelConfig model = controller.getModel(modelId);
            if (model != null && model.getProtocolType() == ModelProtocolType.GROK_RESPONSES) {
                return new GrokModelPickerScreenView(
                        context,
                        model,
                        new GrokModelPickerScreenView.Listener() {
                            @Override
                            public void onBack() {
                                view.handleScreenBack();
                            }

                            @Override
                            public void onSave(ModelConfig updated) {
                                controller.onModelSaved(updated);
                            }
                        }
                );
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
}
