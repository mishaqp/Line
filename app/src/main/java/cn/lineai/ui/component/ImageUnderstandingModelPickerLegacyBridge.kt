package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.model.ModelConfig
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView

object ImageUnderstandingModelPickerLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.ImageUnderstandingModel

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : ImageUnderstandingModelPickerLegacyGateway {
            override fun models(): List<ModelConfig> = controller.models.orEmpty()

            override fun selectedInternalId(): String =
                controller.imageUnderstandingModelId.orEmpty()

            override fun selectImageUnderstandingModel(internalId: String) {
                controller.onImageUnderstandingModelSelected(internalId)
            }
        }

        return ImageUnderstandingModelPickerHostView(
            context = context,
            repository = ImageUnderstandingModelPickerControllerRepository(gateway),
            listener = object : ImageUnderstandingModelPickerHostView.Listener {
                override fun onBack() {
                    view.handleScreenBack()
                }
            }
        )
    }
}
