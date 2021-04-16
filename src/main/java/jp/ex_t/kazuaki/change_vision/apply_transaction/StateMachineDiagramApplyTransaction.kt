/*
 * StateMachineDiagramApplyTransaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.INamedElement
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

    override fun apply(operations: List<StateMachineDiagramOperation>) {
        operations.forEach {
            when (it) {
                is CreateStateMachineDiagram -> {
                    validateAndCreateStateMachineDiagram(it)
                }
                is CreatePseudostate -> validateAndCreatePseudostate(it)
                is ResizePseudostate -> validateAndResizePseudostate(it)
            }
        }
    }

    private fun validateAndCreateStateMachineDiagram(operation: CreateStateMachineDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty() && operation.id.isNotEmpty()) {
            createStateMachineDiagram(operation.name, operation.ownerName, operation.id)
        }
    }

    private fun validateAndCreatePseudostate(operation: CreatePseudostate) {
        if (operation.id.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            createPseudostate(operation.id, location, operation.size, operation.parentId)
        }
    }

    private fun validateAndResizePseudostate(operation: ResizePseudostate) {
        if (operation.id.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            resizePseudostate(operation.id, location, operation.size)
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
        parentId: String
    ) {
        logger.debug("Create pseudostate.")
        val (width, height) = size
        stateMachineDiagramEditor.diagram = diagramViewManager.currentDiagram
        val parentEntity = if (parentId.isEmpty()) null else entityLUT.entries.find { it.common == parentId } ?: run {
            logger.debug("$parentId not found on LUT.")
            return
        }
        val parentPresentation =
            if (parentEntity == null) null else diagramViewManager.currentDiagram.presentations.find { it.id == parentEntity.mine } as INodePresentation?
                ?: run {
                    logger.debug("INodePresentation ${parentEntity.mine} not found but $parentId found on LUT.")
                    return
                }

        val pseudostate = stateMachineDiagramEditor.createInitialPseudostate(parentPresentation, location)
        entityLUT.entries.add(Entry(pseudostate.id, id))
        pseudostate.width = width
        pseudostate.height = height
    }

    private fun resizePseudostate(id: String, location: Point2D, size: Pair<Double, Double>) {
        logger.debug("Resize pseudostate.")
        val (width, height) = size
        stateMachineDiagramEditor.diagram = diagramViewManager.currentDiagram
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val pseudostate = diagramViewManager.currentDiagram.presentations.find { it.id == entry.mine } as INodePresentation? ?: run {
            logger.debug("INodePresentation ${entry.mine} not found but $id found on LUT.")
            return
        }
        pseudostate.location = location
        pseudostate.width = width
        pseudostate.height = height
    }

    companion object : Logging {
        private val logger = logger()
    }
}