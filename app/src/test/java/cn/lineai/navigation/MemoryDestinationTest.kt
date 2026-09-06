package cn.lineai.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class MemoryDestinationTest {
    @Test
    fun memoryRouteUsesTypedDestinationAndSettingsParent() {
        assertEquals(
            LineDestination.Memory,
            LineDestinations.fromScreenId("memory")
        )
        assertEquals(
            LineDestination.Settings,
            LineDestinations.parentOf(LineDestination.Memory)
        )
    }
}
