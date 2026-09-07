package cn.lineai.ui.model

import cn.lineai.R
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LicensesViewModelTest {
    @Test
    fun stateFlowContainsExactlyFourLicensesInLegacyOrder() {
        val viewModel = LicensesViewModel()
        val state: StateFlow<LicensesUiState> = viewModel.state

        assertEquals(4, state.value.licenses.size)
        assertEquals(
            listOf(
                R.string.screen_licenses_commonmark_core,
                R.string.screen_licenses_commonmark_gfm,
                R.string.screen_licenses_jsch,
                R.string.screen_licenses_lucide
            ),
            state.value.licenses.map { it.titleResId }
        )
    }

    @Test
    fun stateKeepsExactLicenseMetadata() {
        val licenses = LicensesViewModel().state.value.licenses

        assertEquals(
            listOf(
                "org.commonmark:commonmark:0.28.0 · BSD-2-Clause",
                "org.commonmark:commonmark-ext-gfm-tables:0.28.0 · BSD-2-Clause",
                "com.github.mwiede:jsch:2.28.2 · Revised BSD / ISC",
                "lucide-react-native:1.14.0 · ISC / MIT"
            ),
            licenses.map { it.meta }
        )
    }

    @Test
    fun stateKeepsExactDescriptionResourceIds() {
        val licenses = LicensesViewModel().state.value.licenses

        assertEquals(
            listOf(
                R.string.screen_licenses_commonmark_core_desc,
                R.string.screen_licenses_commonmark_gfm_desc,
                R.string.screen_licenses_jsch_desc,
                R.string.screen_licenses_lucide_desc
            ),
            licenses.map { it.descriptionResId }
        )
    }

    @Test
    fun backActionDoesNotChangeState() {
        val viewModel = LicensesViewModel()
        val before = viewModel.state.value

        val effect = viewModel.onAction(LicensesUiAction.Back)

        assertSame(before, viewModel.state.value)
        assertSame(LicensesUiEffect.Back, effect)
    }
}
