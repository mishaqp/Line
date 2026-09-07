package cn.lineai.ui.component

import android.content.Context
import cn.lineai.model.ModelConfig
import cn.lineai.model.ModelProviderPreset
import cn.lineai.navigation.LineDestination
import cn.lineai.navigation.LineDestinations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelEditorLegacyBoundaryTest {

    @Test
    fun bothPublicConstructorsArePreserved() {
        val four = ModelAddScreenView::class.java.getConstructor(
            Context::class.java,
            ModelProviderPreset::class.java,
            Boolean::class.javaPrimitiveType!!,
            ModelAddScreenView.Listener::class.java
        )
        val five = ModelAddScreenView::class.java.getConstructor(
            Context::class.java,
            ModelProviderPreset::class.java,
            Boolean::class.javaPrimitiveType!!,
            ModelConfig::class.java,
            ModelAddScreenView.Listener::class.java
        )
        assertEquals(4, four.parameterTypes.size)
        assertEquals(5, five.parameterTypes.size)
        assertEquals(
            "cn.lineai.ui.component.ModelEditorHostView",
            ModelEditorHostView::class.java.name
        )
        assertEquals(
            "cn.lineai.ui.component.ModelEditorControllerRepository",
            ModelEditorControllerRepository::class.java.name
        )
        assertEquals(
            "cn.lineai.ui.component.ModelEditorScreenKt",
            Class.forName("cn.lineai.ui.component.ModelEditorScreenKt").name
        )
    }

    @Test
    fun wrapperResolvesTypedDestinationsFromSafeInputs() {
        assertEquals(
            LineDestination.ModelAdd,
            ModelAddScreenView.resolveDestination(null, false, null)
        )
        assertEquals(
            LineDestination.ModelAddLocal,
            ModelAddScreenView.resolveDestination(null, true, null)
        )
        val preset = ModelProviderPreset(
            "openrouter",
            cn.lineai.model.ModelProtocolType.OPENAI_COMPATIBLE,
            "https://openrouter.ai/api/v1",
            ""
        )
        assertEquals(
            LineDestination.ModelAddPreset("openrouter"),
            ModelAddScreenView.resolveDestination(preset, false, null)
        )
        val editing = ModelEditorViewModelTestSupport.config("model-9")
        assertEquals(
            LineDestination.ModelEdit("model-9"),
            ModelAddScreenView.resolveDestination(null, false, editing)
        )
    }

    @Test
    fun screenFactoriesKeepModelAddContracts() {
        assertEquals("modelAdd", ScreenFactories.ModelAddScreenFactory().screenId())
        assertEquals("modelAdd:local", ScreenFactories.ModelAddLocalScreenFactory().screenId())
        assertEquals("modelAdd:preset:", ScreenFactories.ModelAddPresetScreenFactory().screenId())
        assertEquals("modelEdit:", ScreenFactories.ModelEditScreenFactory().screenId())
        assertEquals(
            "cn.lineai.ui.component.ScreenFactories",
            ScreenFactories::class.java.name
        )
        assertTrue(ScreenFactories.ModelAddPresetScreenFactory().matches("modelAdd:preset:openrouter"))
        assertTrue(ScreenFactories.ModelEditScreenFactory().matches("modelEdit:abc"))
        assertFalse(ScreenFactories.ModelAddPresetScreenFactory().matches("modelAdd"))
    }

    @Test
    fun typedRoutesStayOnExpectedParents() {
        assertEquals(LineDestination.ModelAdd, LineDestinations.fromScreenId("modelAdd"))
        assertEquals(LineDestination.ModelAddLocal, LineDestinations.fromScreenId("modelAdd:local"))
        assertTrue(LineDestinations.fromScreenId("modelAdd:preset:deepseek") is LineDestination.ModelAddPreset)
        assertTrue(LineDestinations.fromScreenId("modelEdit:x") is LineDestination.ModelEdit)
        assertEquals(
            LineDestination.ModelAddOptions,
            LineDestinations.parentOf(LineDestination.ModelAdd)
        )
        assertEquals(
            LineDestination.ModelAddOptions,
            LineDestinations.parentOf(LineDestination.ModelAddLocal)
        )
        assertEquals(
            LineDestination.Models,
            LineDestinations.parentOf(LineDestination.ModelEdit("x"))
        )
    }

    @Test
    fun codexAndGrokAccountRoutesStayAccountDestinations() {
        assertEquals(LineDestination.CodexAccount, LineDestinations.fromScreenId("codexAccount"))
        assertEquals(LineDestination.GrokAccount, LineDestinations.fromScreenId("grokAccount"))
        assertFalse(LineDestinations.fromScreenId("codexAccount") is LineDestination.Legacy)
        assertFalse(LineDestinations.fromScreenId("grokAccount") is LineDestination.Legacy)
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.CodexAccount)
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.GrokAccount)
        )
    }
}

private object ModelEditorViewModelTestSupport {
    fun config(id: String): ModelConfig = cn.lineai.ui.model.ModelEditorViewModelTest.config(id = id)
}
