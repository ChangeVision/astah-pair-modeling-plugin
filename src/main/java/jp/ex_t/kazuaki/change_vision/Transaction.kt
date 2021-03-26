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
    var createAssociationModel: CreateAssociationModel? = null,
    var createClassPresentation: CreateClassPresentation? = null,
    var createAssociationPresentation: CreateAssociationPresentation? = null,
    var createOperation: CreateOperation? = null,
    var createTopic: CreateTopic? = null,
    var createFloatingTopic: CreateFloatingTopic? = null,
    var resizeClassPresentation: ResizeClassPresentation? = null,
    var changeOperationNameAndReturnTypeExpression: ChangeOperationNameAndReturnTypeExpression? = null,
    var resizeTopic: ResizeTopic? = null,
    var deleteClassModel: DeleteClassModel? = null,
    var deleteAssociationPresentation: DeleteAssociationPresentation? = null,
) {
    fun isNotAllNull(): Boolean {
        val checkList = listOf(
            createClassDiagram,
            createMindMapDiagram,
            createClassModel,
            createAssociationModel,
            createClassPresentation,
            createAssociationPresentation,
            createOperation,
            createTopic,
            createFloatingTopic,
            resizeClassPresentation,
            changeOperationNameAndReturnTypeExpression,
            resizeTopic,
            deleteClassModel,
            deleteAssociationPresentation,
        )
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
data class CreateAssociationModel(val sourceClassName: String, val destinationClassName: String, val name: String = "")

@Serializable
data class CreateClassPresentation(val className: String, val location: Pair<Double, Double>, val diagramName: String)

@Serializable
data class CreateAssociationPresentation(val sourceClassName: String, val targetClassName: String, val diagramName: String)

@Serializable
data class CreateOperation(val ownerName: String, val name: String, val returnTypeExpression: String)

@Serializable
data class CreateTopic(val ownerName: String, val name: String, val diagramName: String)

@Serializable
data class CreateFloatingTopic(val name: String, val location: Pair<Double, Double>, val size: Pair<Double, Double>, val diagramName: String)

@Serializable
data class ResizeClassPresentation(val className: String, val location: Pair<Double, Double>, val size: Pair<Double, Double>, val diagramName: String)

@Serializable
data class ChangeOperationNameAndReturnTypeExpression(val ownerName: String, val brotherNameAndReturnTypeExpression: List<Pair<String, String>>, val name: String, val returnTypeExpression: String)

@Serializable
data class ResizeTopic(val name: String, val location: Pair<Double, Double>, val size: Pair<Double, Double>, val diagramName: String)

@Serializable
data class DeleteAssociationPresentation(val sourceClassName: String, val targetClassName: String, val diagramName: String)

@Serializable
data class DeleteClassModel(val className: String)