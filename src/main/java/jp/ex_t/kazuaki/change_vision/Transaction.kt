/*
 * Transaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import kotlinx.serialization.Serializable

@Serializable
data class Transaction (
    var createClassDiagram: CreateClassDiagram? = null,
    var createMindMapDiagram: CreateMindMapDiagram? = null,
    var createClassModel: CreateClassModel? = null,
    var createClassPresentation: CreateClassPresentation? = null,
    var resizeClassPresentation: ResizeClassPresentation? = null,
    var createAssociationModel: CreateAssociationModel? = null,
    var createAssociationPresentation: CreateAssociationPresentation? = null,
    var deleteAssociationPresentation: DeleteAssociationPresentation? = null,
    var deleteClassModel: DeleteClassModel? = null
) {
    fun isNotAllNull(): Boolean {
        val checkList = listOf(createClassDiagram, createMindMapDiagram, createClassModel, createClassPresentation, resizeClassPresentation, createAssociationModel, createAssociationPresentation, deleteAssociationPresentation, deleteClassModel)
        return checkList.any { it != null }
    }
}

@Serializable
data class CreateClassDiagram(val name: String, val ownerName: String)

@Serializable
data class CreateMindMapDiagram(val name: String, val ownerName: String)

@Serializable
data class CreateClassModel(val name: String, val parentPackageName: String)

@Serializable
data class CreateClassPresentation(val className: String, val location: Pair<Double, Double>, val diagramName: String)

@Serializable
data class ResizeClassPresentation(val className: String, val location: Pair<Double, Double>, val size: Pair<Double, Double>, val diagramName: String)

@Serializable
data class CreateAssociationModel(val sourceClassName: String, val destinationClassName: String, val name: String = "")

@Serializable
data class CreateAssociationPresentation(val sourceClassName: String, val targetClassName: String, val diagramName: String)

@Serializable
data class DeleteAssociationPresentation(val sourceClassName: String, val targetClassName: String, val diagramName: String)

@Serializable
data class DeleteClassModel(val className: String)