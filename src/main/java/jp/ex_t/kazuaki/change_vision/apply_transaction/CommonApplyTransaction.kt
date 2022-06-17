/*
 * CommonApplyTransaction.kt - pair-modeling
 * Copyright © 2022 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.model.*
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*

class CommonApplyTransaction(private val entityTable: EntityTable) : IApplyTransaction<CommonOperation> {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    private val basicModelEditor = projectAccessor.modelEditorFactory.basicModelEditor

    @Throws(BadTransactionException::class)
    override fun apply(operations: List<CommonOperation>) {
        operations.forEach {
            when (it) {
                is DeleteModel -> validateAndDeleteModel(it)
                is DeletePackage -> validateAndDeletePackage(it)
                is ModifyDiagram -> validateAndModifyClassDiagram(it)
                is ModifyPackage -> validateAndModifyPackage(it)
                is CreatePackage -> validateAndCreatePackage(it)
            }
        }
    }

    private fun validateAndCreatePackage(operation: CreatePackage) {
        if (operation.id.isNotEmpty() && operation.name.isNotEmpty() && operation.ownerId.isNotEmpty()) {
            createPackage(operation.id, operation.name, operation.ownerId)
        }
    }

    private fun validateAndModifyPackage(operation: ModifyPackage) {
        if (operation.id.isNotEmpty() && operation.name.isNotEmpty() && operation.parentId.isNotEmpty()) {
            modifyPackage(operation.id, operation.name, operation.parentId)
        }
    }

    private fun validateAndModifyClassDiagram(operation: ModifyDiagram) {
        if (operation.id.isNotEmpty() && operation.name.isNotEmpty() && operation.ownerId.isNotEmpty()) {
            modifyClassDiagram(operation.id, operation.name, operation.ownerId)
        }
    }

    private fun validateAndDeletePackage(operation: DeletePackage) {
        if (operation.id.isNotEmpty()) {
            deletePackage(operation.id)
        }
    }

    private fun validateAndDeleteModel(operation: DeleteModel) {
        if (operation.id.isNotEmpty()) {
            deleteModel(operation.id)
        }
    }

    private fun createPackage(id: String, name: String, ownerId: String) {
        logger.debug("Create package.")
        val ownerEntry = entityTable.entries.find { it.common == ownerId } ?: run {
            logger.debug("$ownerId not found on LUT.")
            return
        }
        val owner =
            projectAccessor.findElements(INamedElement::class.java).find { it.id == ownerEntry.mine } as IPackage?
                ?: run {
                    logger.debug("INamedElement ${ownerEntry.mine} not found but $ownerId found on LUT.")
                    return
                }
        val newPackage = basicModelEditor.createPackage(owner, name)
        entityTable.entries.add(Entry(newPackage.id, id))
    }

    private fun modifyPackage(id: String, name: String, ownerId: String) {
        logger.debug("Modify package.")
        val entry = entityTable.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val existPackage =
            projectAccessor.findElements(IPackage::class.java).find { it.id == entry.mine } as IPackage? ?: run {
                logger.debug("IPackage ${entry.mine} not found but $id found on LUT.")
                return
            }
        existPackage.name = name
        val ownerEntry = entityTable.entries.find { it.common == ownerId } ?: run {
            logger.debug("owner id $ownerId not found on LUT.")
            return
        }
        val owner = projectAccessor.findElements(INamedElement::class.java).find { it.id == ownerEntry.mine } ?: run {
            logger.debug("INamedElement ${ownerEntry.mine} not found but $ownerId found on LUT.")
            return
        }
        basicModelEditor.changeParent(owner, existPackage)
    }

    private fun modifyClassDiagram(id: String, name: String, ownerId: String) {
        logger.debug("Modify diagram.")
        val diagramEntry = entityTable.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java)
                .find { it.id == diagramEntry.mine } as IDiagram? ?: run {
                logger.debug("IDiagram ${diagramEntry.mine} not found but $id found on LUT.")
                return
            }
        val ownerEntry = entityTable.entries.find { it.common == ownerId } ?: run {
            logger.debug("$ownerId not found on LUT.")
            return
        }
        val owner = projectAccessor.findElements(IElement::class.java).find { it.id == ownerEntry.mine } ?: run {
            logger.debug("IElement ${ownerEntry.mine} not found but $ownerId found on LUT.")
            return
        }
        diagram.name = name
        // TODO: アクティビティ図、データフロー図、ステートマシン図が不具合で変更できないため除外する
        if (diagram is IActivityDiagram || diagram is IDataFlowDiagram || diagram is IStateMachineDiagram) {
            logger.debug("IActivityDiagram, IDataFlowDiagram and IStateMachineDiagram cannot change parent.")
        }
        basicModelEditor.changeParent(owner, diagram)
    }

    private fun deletePackage(id: String) {
        logger.debug("Delete package.")
        val lutEntry = entityTable.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val existPackage = projectAccessor.findElements(IPackage::class.java).find { it.id == lutEntry.mine } ?: run {
            logger.debug("Package ${lutEntry.mine} not found but $id found on LUT.")
            entityTable.entries.remove(lutEntry)
            return
        }
        entityTable.entries.remove(lutEntry)
        basicModelEditor.delete(existPackage)
    }

    private fun deleteModel(id: String) {
        logger.debug("Delete model.")
        val lutEntry = entityTable.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val model = projectAccessor.findElements(IEntity::class.java).find { it.id == lutEntry.mine } ?: run {
            logger.debug("Model ${lutEntry.mine} not found but $id found on LUT.")
            entityTable.entries.remove(lutEntry)
            return
        }
        entityTable.entries.remove(lutEntry)
        basicModelEditor.delete(model)
    }

    companion object : Logging {
        private val logger = logger()
    }
}