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
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.CreateStateMachineDiagram
import jp.ex_t.kazuaki.change_vision.network.EntityLUT
import jp.ex_t.kazuaki.change_vision.network.Entry
import jp.ex_t.kazuaki.change_vision.network.StateMachineDiagramOperation

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
            }
        }
    }

    private fun validateAndCreateStateMachineDiagram(operation: CreateStateMachineDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty() && operation.id.isNotEmpty()) {
            createStateMachineDiagram(operation.name, operation.ownerName, operation.id)
        }
    }

    private fun createStateMachineDiagram(name: String, ownerName: String, id: String) {
        logger.debug("Create state machine diagram.")
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first()
        val diagram = stateMachineDiagramEditor.createStatemachineDiagram(owner, name)
        entityLUT.entries.add(Entry(diagram.id, id))
        diagramViewManager.open(diagram)
    }

    companion object : Logging {
        private val logger = logger()
    }
}