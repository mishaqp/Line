package cn.lineai.mvp;

import org.junit.Assert;
import org.junit.Test;

public final class ScreenNavigationControllerTest {
    @Test
    public void backFallsThroughParentMappingThenChat() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.showScreen("settings", host);
        controller.showScreen("llm", host);
        controller.backFrom("llm", host);
        controller.backFrom("settings", host);

        Assert.assertEquals("settings", host.lastScreenId);
        Assert.assertFalse(host.lastForward);
        Assert.assertTrue(host.chatShown);
    }

    @Test
    public void forwardNavigationMarksForwardDirection() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.showScreen("settings", host);

        Assert.assertEquals("settings", host.lastScreenId);
        Assert.assertTrue(host.lastForward);
        Assert.assertTrue(host.lastAnimate);
    }

    @Test
    public void refreshVisibleScreenDoesNotAnimateCurrentScreenRebuild() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.showScreen("mcp", host);
        controller.refreshVisibleScreen("mcp", host);

        Assert.assertEquals("mcp", host.lastScreenId);
        Assert.assertTrue(host.lastForward);
        Assert.assertFalse(host.lastAnimate);
    }

    @Test
    public void backNavigationAnimatesInReverseDirection() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.showScreen("settings", host);
        controller.showScreen("mcp", host);
        controller.backFrom("mcp", host);

        Assert.assertEquals("settings", host.lastScreenId);
        Assert.assertFalse(host.lastForward);
        Assert.assertTrue(host.lastAnimate);
    }

    @Test
    public void directBackUsesParentScreenWhenStackIsEmpty() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.backFrom("modelEdit:m1", host);

        Assert.assertEquals("models", host.lastScreenId);
        Assert.assertFalse(host.chatShown);
    }

    @Test
    public void promptTemplatesBackReturnsToAiBehavior() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.backFrom("promptTemplates", host);

        Assert.assertEquals("llm", host.lastScreenId);
        Assert.assertFalse(host.chatShown);
    }

    @Test
    public void inputSettingsBackReturnsToSettings() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.backFrom("input", host);

        Assert.assertEquals("settings", host.lastScreenId);
        Assert.assertFalse(host.chatShown);
    }

    @Test
    public void imageUnderstandingModelBackReturnsToToolSettings() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.backFrom("imageUnderstandingModel", host);

        Assert.assertEquals("toolSettings", host.lastScreenId);
        Assert.assertFalse(host.chatShown);
    }

    @Test
    public void advancedFeaturesPhoneControlBackChainUsesExistingStack() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.showScreen("settings", host);
        controller.showScreen("advancedFeatures", host);
        controller.showScreen("phoneControl", host);

        controller.backFrom("phoneControl", host);
        Assert.assertEquals("advancedFeatures", host.lastScreenId);
        Assert.assertFalse(host.lastForward);
        Assert.assertFalse(host.chatShown);

        controller.backFrom("advancedFeatures", host);
        Assert.assertEquals("settings", host.lastScreenId);
        Assert.assertFalse(host.lastForward);
        Assert.assertFalse(host.chatShown);
    }

    @Test
    public void phoneControlDirectBackPreservesLegacyFallbackToChat() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.backFrom("phoneControl", host);

        Assert.assertTrue(host.chatShown);
        Assert.assertEquals("", host.lastScreenId);
    }

    @Test
    public void extensionsBackChainWorksForAllFiveMenuDestinations() {
        String[] childIds = new String[] {
                "extension:agent",
                "extension:mcp",
                "extension:skills",
                "extension:linecode",
                "terminalProvider"
        };

        for (String childId : childIds) {
            ScreenNavigationController controller = new ScreenNavigationController();
            RecordingHost host = new RecordingHost();

            controller.showScreen("settings", host);
            controller.showScreen("extensions", host);
            controller.showScreen(childId, host);

            controller.backFrom(childId, host);
            Assert.assertEquals(childId, "extensions", host.lastScreenId);
            Assert.assertFalse(childId, host.lastForward);
            Assert.assertFalse(childId, host.chatShown);

            controller.backFrom("extensions", host);
            Assert.assertEquals(childId, "settings", host.lastScreenId);
            Assert.assertFalse(childId, host.lastForward);
            Assert.assertFalse(childId, host.chatShown);
        }
    }

    @Test
    public void terminalProviderDirectBackUsesExtensionsFallback() {
        ScreenNavigationController controller = new ScreenNavigationController();
        RecordingHost host = new RecordingHost();

        controller.backFrom("terminalProvider", host);

        Assert.assertEquals("extensions", host.lastScreenId);
        Assert.assertFalse(host.lastForward);
        Assert.assertFalse(host.chatShown);
    }

    private static final class RecordingHost implements ScreenNavigationController.Host {
        private String lastScreenId = "";
        private boolean lastForward;
        private boolean lastAnimate;
        private boolean chatShown;

        @Override
        public void hideOverlays() {
        }

        @Override
        public void showScreen(String screenId) {
            lastScreenId = screenId;
        }

        @Override
        public void showScreen(String screenId, boolean forward) {
            lastScreenId = screenId;
            lastForward = forward;
            lastAnimate = true;
        }

        @Override
        public void showScreen(String screenId, boolean forward, boolean animate) {
            lastScreenId = screenId;
            lastForward = forward;
            lastAnimate = animate;
        }

        @Override
        public void showChatScreen() {
            chatShown = true;
        }
    }
}
