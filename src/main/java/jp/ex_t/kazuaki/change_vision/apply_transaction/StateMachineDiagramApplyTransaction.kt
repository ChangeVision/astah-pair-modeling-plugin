/*
 * StateMachineDiagramApplyTransaction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.model.INamedElement
import com.change_vision.jude.api.inf.model.IStateMachineDiagram
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import java.awt.geom.Point2D

class StateMachineDiagramApplyTransaction(private val entityLUT: EntityLUT) :
    IApplyTransaction<StateMachineDiagramOperation> {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    private val diagramViewManager = api.viewManager.diagramViewManager
    private val stateMachineDiagramEditor = projectAccessor.diagramEditorFactory.stateMachineDiagramEditor
    private val basicModelEditor = projectAccessor.modelEditorFactory.basicModelEditor

    override fun apply(operations: List<StateMachineDiagramOperation>) {
        operations.forEach {
            when (it) {
                is CreateStateMachineDiagram -> {
                    validateAndCreateStateMachineDiagram(it)
                }
                is CreatePseudostate -> validateAndCreatePseudostate(it)
                is CreateState -> validateAndCreateState(it)
                is CreateFinalState -> validateAndCreateFinalState(it)
                is CreateTransition -> validateAndCreateTransition(it)
                is ModifyPseudostate -> validateAndModifyPseudostate(it)
                is ModifyState -> validateAndModifyState(it)
                is ModifyFinalState -> validateAndModifyFinalState(it)
                is ModifyTransition -> validateAndModifyTransition(it)
            }
        }
    }

    private fun validateAndCreateStateMachineDiagram(operation: CreateStateMachineDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty() && operation.id.isNotEmpty()) {
            createStateMachineDiagram(operation.name, operation.ownerName, operation.id)
        }
    }

    private fun validateAndCreatePseudostate(operation: CreatePseudostate) {
        if (operation.id.isNotEmpty() && operation.diagramId.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            createPseudostate(operation.id, location, operation.size, operation.parentId, operation.diagramId)
        }
    }

    private fun validateAndCreateState(operation: CreateState) {
        if (operation.id.isNotEmpty() && operation.diagramId.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            createState(
                operation.id,
                operation.name,
                location,
                operation.size,
                operation.parentId,
                operation.diagramId
            )
        }
    }

    private fun validateAndCreateFinalState(operation: CreateFinalState) {
        if (operation.id.isNotEmpty() && operation.diagramId.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            createFinalState(operation.id, location, operation.size, operation.parentId, operation.diagramId)
        }
    }

    private fun validateAndCreateTransition(operation: CreateTransition) {
        if (operation.id.isNotEmpty() && operation.sourceId.isNotEmpty() && operation.targetId.isNotEmpty() && operation.diagramName.isNotEmpty()) {
            createTransition(
                operation.id,
                operation.label,
                operation.sourceId,
                operation.targetId,
                operation.diagramName
            )
        }
    }

    private fun validateAndModifyPseudostate(operation: ModifyPseudostate) {
        if (operation.id.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            modifyPseudostate(operation.id, location, operation.size, operation.parentId)
        }
    }

    private fun validateAndModifyState(operation: ModifyState) {
        if (operation.id.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            modifyState(operation.id, operation.name, location, operation.size, operation.parentId)
        }
    }

    private fun validateAndModifyFinalState(operation: ModifyFinalState) {
        if (operation.id.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            modifyFinalState(operation.id, location, operation.size, operation.parentId)
        }
    }

    private fun validateAndModifyTransition(operation: ModifyTransition) {
        if (operation.id.isNotEmpty()) {
            modifyTransition(operation.id, operation.label)
        }
    }

    private fun createStateMachineDiagram(name: String, ownerName: String, id: String) {
        logger.debug("Create state machine diagram.")
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first()
        val diagram = stateMachineDiagramEditor.createStatemachineDiagram(owner, name)
        entityLUT.entries.add(Entry(diagram.id, id))
        diagramViewManager.open(diagram)
    }

    private fun createPseudostate(
        id: String,
        location: Point2D,
        size: Pair<Double, Double>,
        parentId: String,
        diagramId: String
    ) {
        logger.debug("Create pseudostate.")
        val (width, height) = size
        val diagramEntry = entityLUT.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java)
                .find { it.id == diagramEntry.mine } as IStateMachineDiagram?
                ?: run {
                    logger.debug("IStateMachineDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        stateMachineDiagramEditor.diagram = diagram
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val pseudostate = stateMachineDiagramEditor.createInitialPseudostate(parentPresentation, location)
        entityLUT.entries.add(Entry(pseudostate.model.id, id))
        pseudostate.width = width
        pseudostate.height = height
    }

    private fun createState(
        id: String,
        name: String,
        location: Point2D,
        size: Pair<Double, Double>,
        parentId: String,
        diagramId: String
    ) {
        logger.debug("Create state.")
        val (width, height) = size
        val diagramEntry = entityLUT.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java)
                .find { it.id == diagramEntry.mine } as IStateMachineDiagram?
                ?: run {
                    logger.debug("IStateMachineDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        stateMachineDiagramEditor.diagram = diagram
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val state = stateMachineDiagramEditor.createState(name, parentPresentation, location)
        entityLUT.entries.add(Entry(state.model.id, id))
        state.width = width
        state.height = height
    }

    private fun createFinalState(
        id: String,
        location: Point2D,
        size: Pair<Double, Double>,
        parentId: String,
        diagramId: String
    ) {
        logger.debug("Create FinalState.")
        val (width, height) = size
        val diagramEntry = entityLUT.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java)
                .find { it.id == diagramEntry.mine } as IStateMachineDiagram?
                ?: run {
                    logger.debug("IStateMachineDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        stateMachineDiagramEditor.diagram = diagram
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val pseudostate = stateMachineDiagramEditor.createFinalState(parentPresentation, location)
        entityLUT.entries.add(Entry(pseudostate.model.id, id))
        pseudostate.width = width
        pseudostate.height = height
    }

    private fun createTransition(id: String, label: String, sourceId: String, targetId: String, diagramName: String) {
        logger.debug("Create transition.")
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        stateMachineDiagramEditor.diagram = diagram
        val sourceEntry = entityLUT.entries.find { it.common == sourceId } ?: run {
            logger.debug("$sourceId not found on LUT.")
            return
        }
        val sourcePresentation =
            diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == sourceEntry.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${sourceEntry.mine} not found but $sourceId found on LUT.")
                    return
                }
        val targetEntry = entityLUT.entries.find { it.common == targetId } ?: run {
            logger.debug("$targetId not found on LUT.")
            return
        }
        val targetPresentation =
            diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == targetEntry.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${targetEntry.mine} not found but $targetId found on LUT.")
                    return
                }
        val transition = stateMachineDiagramEditor.createTransition(sourcePresentation, targetPresentation)
        entityLUT.entries.add(Entry(transition.model.id, id))
        // Because of APIs specification, label must be set as model name.
        (transition.model as INamedElement).name = label
    }

    private fun modifyPseudostate(id: String, location: Point2D, size: Pair<Double, Double>, parentId: String) {
        logger.debug("Modify pseudostate.")
        val (width, height) = size
        stateMachineDiagramEditor.diagram = diagramViewManager.currentDiagram
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val pseudostate =
            diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == entry.mine } as INodePresentation? ?: run {
                logger.debug("INodePresentation ${entry.mine} not found but $id found on LUT.")
                return
            }
        if (pseudostate.parent != parentPresentation) {
            stateMachineDiagramEditor.changeParentOfState(pseudostate, parentPresentation)
        }
        pseudostate.location = location
        pseudostate.width = width
        pseudostate.height = height
    }

    private fun modifyState(id: String, name: String, location: Point2D, size: Pair<Double, Double>, parentId: String) {
        logger.debug("Modify state.")
        val (width, height) = size
        stateMachineDiagramEditor.diagram = diagramViewManager.currentDiagram
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val state =
            diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == entry.mine } as INodePresentation? ?: run {
                logger.debug("INodePresentation ${entry.mine} not found but $id found on LUT.")
                return
            }
        if (state.parent != parentPresentation) {
            stateMachineDiagramEditor.changeParentOfState(state, parentPresentation)
        }
        (state.model as INamedElement).name = name
        state.location = location
        state.width = width
        state.height = height
    }

    private fun modifyFinalState(id: String, location: Point2D, size: Pair<Double, Double>, parentId: String) {
        logger.debug("Modify final state.")
        val (width, height) = size
        stateMachineDiagramEditor.diagram = diagramViewManager.currentDiagram
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val finalState =
            diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == entry.mine } as INodePresentation? ?: run {
                logger.debug("INodePresentation ${entry.mine} not found but $id found on LUT.")
                return
            }
        if (finalState.parent != parentPresentation) {
            stateMachineDiagramEditor.changeParentOfState(finalState, parentPresentation)
        }
        finalState.location = location
        finalState.width = width
        finalState.height = height
    }

    private fun modifyTransition(id: String, label: String) {
        logger.debug("Modify transition.")
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val transition =
            diagramViewManager.currentDiagram.presentations.filterNot { it.model == null }
                .find { it.model.id == entry.mine } as ILinkPresentation? ?: run {
                logger.debug("ILinkPresentation ${entry.mine} not found but $id found on LUT.")
                return
            }
        (transition.model as INamedElement).name = label
    }

    companion object : Logging {
        private val logger = logger()
    }
}