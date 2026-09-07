package cn.lineai.ui.component

import android.content.Context
import android.view.View
import cn.lineai.model.ModelConfig
import cn.lineai.mvp.MainUiController
import cn.lineai.navigation.LineDestination
import cn.lineai.ui.MainChatView

object ImageGenerationModelPickerLegacyBridge {
    @JvmStatic
    fun handles(destination: LineDestination?): Boolean =
        destination is LineDestination.ImageGenerationModel

    @JvmStatic
    fun create(
        context: Context,
        view: MainChatView,
        controller: MainUiController
    ): View {
        val gateway = object : ImageGenerationModelPickerLegacyGateway {
            override fun models(): List<ModelConfig> = controller.models.orEmpty()

            override fun selectedInternalId(): String =
                controller.imageGenerationModelId.orEmpty()

            override fun selectImageGenerationModel(internalId: String) {
                controller.onImageGenerationModelSelected(internalId)
            }
        }

        return ImageGenerationModelPickerHostView(
            context = context,
            repository = ImageGenerationModelPickerControllerRepository(gateway),
            listener = object : ImageGenerationModelPickerHostView.Listener {
                override fun onBack() {
                    view.handleScreenBack()
                }
            }
        )
    }
}
