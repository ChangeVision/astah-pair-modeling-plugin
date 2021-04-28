/*
 * Transaction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(val operations: List<TransactionalOperation>)

@Serializable
sealed class TransactionalOperation

@Serializable
sealed class ClassDiagramOperation : TransactionalOperation()

@Serializable
sealed class MindmapDiagramOperation : TransactionalOperation()

@Serializable
sealed class StateMachineDiagramOperation : TransactionalOperation()

@Serializable
data class CreateClassDiagram(
    val name: String,
    val ownerName: String,
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateMindmapDiagram(
    val name: String,
    val ownerName: String,
    val rootTopicId: String,
) : MindmapDiagramOperation()

@Serializable
data class CreateStateMachineDiagram(
    val name: String,
    val ownerName: String,
    val id: String,
) : StateMachineDiagramOperation()

@Serializable
data class CreateClassModel(
    val name: String,
    val parentName: String,
    val stereotypes: List<String?> = mutableListOf(),
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateAssociationModel(
    val sourceClassId: String,
    val sourceClassNavigability: String,
    val destinationClassId: String,
    val destinationClassNavigability: String,
    val name: String = "",
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateGeneralizationModel(
    val superClassId: String,
    val subClassId: String,
    val name: String = "",
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateRealizationModel(
    val supplierClassId: String,
    val clientClassId: String,
    val name: String = "",
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreatePseudostate(
    val id: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String,
) : StateMachineDiagramOperation()

@Serializable
data class CreateState(
    val id: String,
    val name: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String,
) : StateMachineDiagramOperation()

@Serializable
data class CreateFinalState(
    val id: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String,
) : StateMachineDiagramOperation()

@Serializable
data class CreateTransition(
    val id: String,
    val label: String,
    val sourceId: String,
    val targetId: String,
) : StateMachineDiagramOperation()

@Serializable
data class CreateClassPresentation(
    val classId: String,
    val location: Pair<Double, Double>,
    val diagramName: String,
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateLinkPresentation(
    val sourceClassId: String,
    val targetClassId: String,
    val linkType: LinkType,
    val diagramName: String,
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateNote(
    val note: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String,
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateOperation(
    val ownerId: String,
    val name: String,
    val returnTypeExpression: String,
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateAttribute(
    val ownerId: String,
    val name: String,
    val typeExpression: String,
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class CreateTopic(
    val ownerId: String,
    val name: String,
    val diagramName: String,
    val id: String,
) : MindmapDiagramOperation()

@Serializable
data class CreateFloatingTopic(
    val name: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String,
    val id: String,
) : MindmapDiagramOperation()

@Serializable
data class ModifyClassPresentation(
    val id: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String,
) : ClassDiagramOperation()

@Serializable
data class ModifyNote(
    val id: String,
    val note: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val diagramName: String
) : ClassDiagramOperation()

@Serializable
data class ModifyClassModel(
    val id: String,
    val name: String,
    val stereotypes: List<String?> = mutableListOf(),
) : ClassDiagramOperation()

@Serializable
data class ModifyOperation(
    val ownerId: String,
    val id: String,
    val name: String,
    val returnTypeExpression: String,
) : ClassDiagramOperation()

@Serializable
data class ModifyAttribute(
    val ownerId: String,
    val id: String,
    val name: String,
    val typeExpression: String,
) : ClassDiagramOperation()

@Serializable
data class ResizeTopic(
    val name: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String,
    val id: String,
) : MindmapDiagramOperation()

@Serializable
data class ModifyPseudostate(
    val id: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String,
) : StateMachineDiagramOperation()

@Serializable
data class ModifyState(
    val id: String,
    val name: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String,
) : StateMachineDiagramOperation()

@Serializable
data class ModifyFinalState(
    val id: String,
    val location: Pair<Double, Double>,
    val size: Pair<Double, Double>,
    val parentId: String
) : StateMachineDiagramOperation()

@Serializable
data class ModifyTransition(
    val id: String,
    val label: String,
) : StateMachineDiagramOperation()

@Serializable
data class DeleteModel(
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class DeletePresentation(
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class DeleteTopic(
    val id: String,
) : MindmapDiagramOperation()
@Serializable
data class DeleteNote(
    val id: String,
) : ClassDiagramOperation()

@Serializable
data class DeleteStateMachineNodePresentation(
    val id: String,
) : StateMachineDiagramOperation()

@Serializable
data class DeleteStateMachineLinkPresentation(
    val id: String,
) : StateMachineDiagramOperation()