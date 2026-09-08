package cn.lineai.ui.component

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import cn.lineai.model.FileTreeNode
import cn.lineai.ui.model.DrawerRepository
import cn.lineai.ui.model.DrawerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerLegacyBoundaryTest {

    @Test
    fun drawerViewKeepsTheExactLegacyPublicApi() {
        DrawerView::class.java.getConstructor(Context::class.java)
        DrawerView::class.java.getMethod(
            "setListener",
            DrawerView.Listener::class.java
        )
        DrawerView::class.java.getMethod(
            "render",
            List::class.java,
            String::class.java,
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType,
            FileTreeNode::class.java
        )
        DrawerView::class.java.getMethod("open")
        DrawerView::class.java.getMethod("close")
        DrawerView::class.java.getMethod("isFilesTabActive")

        assertTrue(FrameLayout::class.java.isAssignableFrom(DrawerView::class.java))
    }

    @Test
    fun listenerKeepsAllNineMainChatCallbacks() {
        val signatures = DrawerView.Listener::class.java.declaredMethods
            .associate { method ->
                method.name to method.parameterTypes.toList()
            }

        assertEquals(9, signatures.size)
        assertEquals(emptyList<Class<*>>(), signatures["onCloseDrawer"])
        assertEquals(emptyList<Class<*>>(), signatures["onNewConversation"])
        assertEquals(listOf(String::class.java), signatures["onConversationSelected"])
        assertEquals(listOf(String::class.java), signatures["onConversationDeleted"])
        assertEquals(emptyList<Class<*>>(), signatures["onCurrentProjectRemoveRequested"])
        assertEquals(
            listOf(String::class.java, Boolean::class.javaPrimitiveType),
            signatures["onFileNodeSelected"]
        )
        assertEquals(
            listOf(
                String::class.java,
                String::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            ),
            signatures["onFileNodeLongPressed"]
        )
        assertEquals(emptyList<Class<*>>(), signatures["onFileTreeActivated"])
        assertEquals(emptyList<Class<*>>(), signatures["onFileTreeRefresh"])
    }

    @Test
    fun viewModelAndRepositoryBoundaryContainNoAndroidOrLegacyControllerTypes() {
        val viewModelConstructorTypes = DrawerViewModel::class.java.declaredConstructors
            .flatMap { it.parameterTypes.toList() }
        assertTrue(viewModelConstructorTypes.contains(DrawerRepository::class.java))
        assertFalse(viewModelConstructorTypes.any(::isForbiddenType))

        val repositoryFieldTypes = DrawerControllerRepository::class.java.declaredFields
            .map { it.type }
        assertFalse(repositoryFieldTypes.any(::isForbiddenType))
    }

    @Test
    fun hostOwnsViewModelStoreAndRetainsListenerAsAProperty() {
        val fieldNames = DrawerHostView::class.java.declaredFields.map { it.name }
        assertTrue(fieldNames.contains("hostViewModelStore"))
        assertTrue(fieldNames.contains("hostViewModelStoreOwner"))
        assertTrue(fieldNames.contains("listener"))
        assertTrue(fieldNames.contains("closePending"))
    }

    @Test
    fun wrapperStartsAsAViewBoundaryRatherThanASecondNavigationDestination() {
        assertTrue(View::class.java.isAssignableFrom(DrawerView::class.java))
        val wrapperFields = DrawerView::class.java.declaredFields.map { it.type }
        assertTrue(wrapperFields.contains(DrawerHostView::class.java))
        assertTrue(wrapperFields.contains(DrawerControllerRepository::class.java))
    }

    private fun isForbiddenType(type: Class<*>): Boolean {
        val name = type.name
        return name == "android.content.Context" ||
            name == "android.app.Activity" ||
            name == "android.view.View" ||
            name == "cn.lineai.ui.MainUiController" ||
            name.endsWith(".MainUiController")
    }
}
