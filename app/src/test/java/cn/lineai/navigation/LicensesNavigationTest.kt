package cn.lineai.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class LicensesNavigationTest {
    @Test
    fun licensesRouteUsesTypedDestination() {
        assertEquals(
            LineDestination.Licenses,
            LineDestinations.fromScreenId("licenses")
        )
    }

    @Test
    fun licensesParentsToAbout() {
        assertEquals(
            LineDestination.About,
            LineDestinations.parentOf(LineDestination.Licenses)
        )
    }

    @Test
    fun aboutParentsToSettings() {
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.About)
        )
    }
}
