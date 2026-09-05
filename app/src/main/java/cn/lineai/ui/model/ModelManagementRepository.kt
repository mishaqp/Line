package cn.lineai.ui.model

import cn.lineai.model.ModelConfig

/**
 * Android-free boundary for the model list. ViewModels talk to this
 * repository instead of MainUiController, Context or Resources.
 */
interface ModelManagementRepository {
    fun models(): List<ModelConfig>

    fun selectedModelId(): String

    fun selectModel(id: String)

    fun deleteModels(ids: List<String>)
}
