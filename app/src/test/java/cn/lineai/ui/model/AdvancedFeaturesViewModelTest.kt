package cn.lineai.ui.model

import cn.lineai.navigation.LineDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AdvancedFeaturesViewModelTest {
    @Test
    fun catalogContainsExactlyPhoneControlCard() {
        val items = AdvancedFeaturesCatalog.items()

        assertEquals(1, items.size)
        assertEquals(AdvancedFeatureKind.PHONE_CONTROL, items.single().kind)
        assertEquals(LineDestination.PhoneControl, items.single().destination)
    }

    @Test
    fun backProducesBackEffect() {
        val viewModel = AdvancedFeaturesViewModel()

        assertSame(
            AdvancedFeaturesUiEffect.Back,
            viewModel.onAction(AdvancedFeaturesUiAction.Back)
        )
    }

    @Test
    fun openingPhoneControlProducesTypedNavigationEffect() {
        val viewModel = AdvancedFeaturesViewModel()

        assertEquals(
            AdvancedFeaturesUiEffect.Navigate(LineDestination.PhoneControl),
            viewModel.onAction(
                AdvancedFeaturesUiAction.Open(LineDestination.PhoneControl)
            )
        )
    }
}
