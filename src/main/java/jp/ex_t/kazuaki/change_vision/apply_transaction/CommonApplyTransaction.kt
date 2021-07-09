/*
 * CommonApplyTransaction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.model.IElement
import com.change_vision.jude.api.inf.model.IEntity
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.CommonOperation
import jp.ex_t.kazuaki.change_vision.network.DeleteModel
import jp.ex_t.kazuaki.change_vision.network.EntityLUT
import jp.ex_t.kazuaki.change_vision.network.ModifyDiagram

class CommonApplyTransaction(private val entityLUT: EntityLUT) : IApplyTransaction<CommonOperation> {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    private val basicModelEditor = projectAccessor.modelEditorFactory.basicModelEditor

    @Throws(BadTransactionException::class)
    override fun apply(operations: List<CommonOperation>) {
        operations.forEach {
            when (it) {
                is DeleteModel -> validateAndDeleteModel(it)
                is ModifyDiagram -> validateAndModifyClassDiagram(it)
            }
        }
    }

    private fun validateAndModifyClassDiagram(operation: ModifyDiagram) {
        if (operation.id.isNotEmpty() && operation.name.isNotEmpty() && operation.ownerId.isNotEmpty()) {
            modifyClassDiagram(operation.id, operation.name, operation.ownerId)
        }
    }

    private fun validateAndDeleteModel(operation: DeleteModel) {
        if (operation.id.isNotEmpty()) {
            deleteModel(operation.id)
        }
    }

    private fun modifyClassDiagram(id: String, name: String, ownerId: String) {
        logger.debug("Modify diagram.")
        val diagramEntry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java)
                .find { it.id == diagramEntry.mine } as IDiagram? ?: run {
                logger.debug("IDiagram ${diagramEntry.mine} not found but $id found on LUT.")
                return
            }
        val ownerEntry = entityLUT.entries.find { it.common == ownerId } ?: run {
            logger.debug("$ownerId not found on LUT.")
            return
        }
        val owner = projectAccessor.findElements(IElement::class.java).find { it.id == ownerEntry.mine } ?: run {
            logger.debug("IElement ${ownerEntry.mine} not found but $ownerId found on LUT.")
            return
        }
        diagram.name = name
        // TODO: ステートマシン図で動作しない原因を調査する
//        basicModelEditor.changeParent(owner, diagram)
    }

    private fun deleteModel(id: String) {
        logger.debug("Delete model.")
        val lutEntry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val model = projectAccessor.findElements(IEntity::class.java).find { it.id == lutEntry.mine } ?: run {
            logger.debug("Model ${lutEntry.mine} not found but $id found on LUT.")
            entityLUT.entries.remove(lutEntry)
            return
        }
        entityLUT.entries.remove(lutEntry)
        basicModelEditor.delete(model)
    }

    companion object : Logging {
        private val logger = logger()
    }
}