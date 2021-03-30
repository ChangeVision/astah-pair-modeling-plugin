/*
 * Transaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(val operations: MutableList<TransactionalOperation> = mutableListOf())

@Serializable
sealed class TransactionalOperation

@Serializable
sealed class ClassDiagramOperation: TransactionalOperation()

@Serializable
sealed class MindmapDiagramOperation: TransactionalOperation()

@Serializable
data class CreateClassDiagram(
    val name: String,
    val ownerName: String,
): ClassDiagramOperation()

@Serializable
data class CreateMindmapDiagram(
    val name: String,
    val ownerName: String,
): MindmapDiagramOperation()

@Serializable
data class CreateClassModel(
    val name: String,
    val parentPackageName: String,
): ClassDiagramOperation()

@Serializable
data class CreateAssociationModel(
    val sourceClassName: String,
    val destinationClassName: String,
    val name: String = "",
): ClassDiagramOperation()

@Serializable
data class CreateClassPresentation(
    val className: String,
    val location: Pair<Double, Double>,
    val diagramName: String,
): ClassDiagramOperation()

@Serializable
data class CreateAssociationPresentation(
    val sourceClassName: String,
    val targetClassName: String,
    val diagramName: String,
): ClassDiagramOperation()

@Serializable
data class CreateOperation(
    val ownerName: String,
    val name: String,
    val returnTypeExpression: String,
): ClassDiagramOperation()

@Serializable
data class CreateAttribute(
    val ownerName: String,
    val name: String,
    val typeExpression: String,
): ClassDiagramOperation()

@Serializable
data class CreateTopic(
    val ownerName: String,
    val name: String,
    val diagramName: String,
): MindmapDiagramOperation()

@Serializable
data class CreateFloatingTopic(
    val name: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String,
): MindmapDiagramOperation()

@Serializable
data class ResizeClassPresentation(
    val className: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String,
): ClassDiagramOperation()

@Serializable
data class ChangeOperationNameAndReturnTypeExpression(
    val ownerName: String,
    val brotherNameAndReturnTypeExpression: List<Pair<String, String>>,
    val name: String,
    val returnTypeExpression: String,
): ClassDiagramOperation()

@Serializable
data class ChangeAttributeNameAndTypeExpression(
    val ownerName: String,
    val brotherNameAndTypeExpression: List<Pair<String, String>>,
    val name: String,
    val typeExpression: String,
): ClassDiagramOperation()

@Serializable
data class ResizeTopic(
    val name: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String,
): MindmapDiagramOperation()

@Serializable
data class DeleteClassModel(
    val className: String,
): ClassDiagramOperation()

@Serializable
data class DeleteClassPresentation(
    val className: String,
): ClassDiagramOperation()

@Serializable
data class DeleteAssociationModel(
    val isDeleteAssociationModel: Boolean = false,
): ClassDiagramOperation()

@Serializable
data class DeleteAssociationPresentation(
    val points: List<Pair<Double, Double>>,
): ClassDiagramOperation()
