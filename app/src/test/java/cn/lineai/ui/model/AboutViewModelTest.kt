package cn.lineai.ui.model

import cn.lineai.navigation.LineDestination
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AboutViewModelTest {
    @Test
    fun loadsAppLabelFromRepository() {
        val viewModel = AboutViewModel(
            FakeAboutRepository(AboutAppInfo(appLabel = "Line Test"))
        )

        assertEquals("Line Test", viewModel.state.value.appLabel)
    }

    @Test
    fun loadsVersionNameAndVersionCodeFromRepository() {
        val viewModel = AboutViewModel(
            FakeAboutRepository(
                AboutAppInfo(
                    appLabel = "Line Test",
                    versionName = "2.7.1",
                    versionCode = 271L
                )
            )
        )

        assertEquals("2.7.1", viewModel.state.value.versionName)
        assertEquals(271L, viewModel.state.value.versionCode)
    }

    @Test
    fun stateIsStateFlowAndExposesLoadedState() {
        val viewModel = AboutViewModel(
            FakeAboutRepository(
                AboutAppInfo(
                    appLabel = "Line Test",
                    versionName = "3.0",
                    versionCode = 300L
                )
            )
        )

        val state: StateFlow<AboutUiState> = viewModel.state
        assertEquals(
            AboutUiState("Line Test", "3.0", 300L),
            state.value
        )
    }

    @Test
    fun backActionReturnsBackEffect() {
        val effect = viewModel().onAction(AboutUiAction.Back)

        assertSame(AboutUiEffect.Back, effect)
    }

    @Test
    fun openGithubActionReturnsOpenGithubEffect() {
        val effect = viewModel().onAction(AboutUiAction.OpenGithub)

        assertSame(AboutUiEffect.OpenGithub, effect)
    }

    @Test
    fun openLicensesReturnsTypedLicensesDestination() {
        val effect = viewModel().onAction(AboutUiAction.OpenLicenses)

        val open = effect as AboutUiEffect.OpenDestination
        assertSame(LineDestination.Licenses, open.destination)
    }

    @Test
    fun unknownVersionDataFallsBackWithoutBreakingState() {
        val viewModel = AboutViewModel(
            FakeAboutRepository(AboutAppInfo(appLabel = null, versionName = null, versionCode = null))
        )

        assertEquals("LineCode Pro", viewModel.state.value.appLabel)
        assertEquals("unknown", viewModel.state.value.versionName)
        assertEquals(0L, viewModel.state.value.versionCode)
    }

    @Test
    fun repositoryFailureFallsBackWithoutBreakingState() {
        val viewModel = AboutViewModel(
            object : AboutRepository {
                override fun loadAppInfo(): AboutAppInfo = error("unavailable")
            }
        )

        assertEquals(AboutUiState(), viewModel.state.value)
    }

    private fun viewModel(): AboutViewModel =
        AboutViewModel(FakeAboutRepository(AboutAppInfo()))

    private class FakeAboutRepository(
        private val info: AboutAppInfo
    ) : AboutRepository {
        override fun loadAppInfo(): AboutAppInfo = info
    }
}
