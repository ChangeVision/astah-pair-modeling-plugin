/*
 * CommonEventListener.kt - pair-modeling
 * Copyright © 2022 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.model.INamedElement
import com.change_vision.jude.api.inf.model.IPackage
import com.change_vision.jude.api.inf.project.ProjectEditUnit
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi

class CommonEventListener(private val entityTable: EntityTable, private val mqttPublisher: MqttPublisher) :
    IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        val removeOperations = removeProjectEditUnit.mapNotNull { editUnit ->
            Operation.values()[editUnit.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = editUnit.entity) {
                is IPackage -> {
                    deletePackage(entity)
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                    null
                }
            }
        }
        if (removeOperations.isNotEmpty()) {
            val removeTransaction = Transaction(removeOperations)
            ProjectChangedListener.encodeAndPublish(removeTransaction, mqttPublisher)
            return
        }

        val createProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        val createOperations = createProjectEditUnit.mapNotNull { editUnit ->
            Operation.values()[editUnit.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = editUnit.entity) {
                is IPackage -> {
                    createPackage(entity)
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                    null
                }
            }

        }
        if (createOperations.isNotEmpty()) {
            val createTransaction = Transaction(createOperations)
            ProjectChangedListener.encodeAndPublish(createTransaction, mqttPublisher)
            return
        }

        val modifyProjectEditUnit = projectEditUnit.filter { it.operation == Operation.MODIFY.ordinal }
        val modifyOperations = modifyProjectEditUnit.mapNotNull { editUnit ->
            Operation.values()[editUnit.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = editUnit.entity) {
                is IPackage -> {
                    modifyPackage(entity)
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                    null
                }
            }
        }
        if (modifyOperations.isNotEmpty()) {
            val modifyTransaction = Transaction(modifyOperations)
            ProjectChangedListener.encodeAndPublish(modifyTransaction, mqttPublisher)
        }
    }

    private fun createPackage(entity: IPackage): CreatePackage? {
        val owner = entity.owner as INamedElement?
        val parentEntry =
            if (owner == null) Entry("", "") else entityTable.entries.find { it.mine == owner.id }
                ?: run {
                    logger.debug("${owner.id}(Owner of IPackage) not found on LUT.")
                    return null
                }
        val entry = Entry(entity.id, entity.id)
        entityTable.entries.add(entry)
        val createPackage = CreatePackage(entity.name, parentEntry.common, entry.common)
        return createPackage
    }

    private fun modifyPackage(entity: IPackage): ModifyPackage? {
        val owner = entity.owner as INamedElement?
        val parentEntry =
            if (owner == null) Entry("", "") else entityTable.entries.find { it.mine == owner.id } ?: run {
                logger.debug("${owner.id}(Owner of IPackage) not found on LUT.")
                return null
            }
        val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IPackage) not found on LUT.")
            return null
        }
        return ModifyPackage(entry.common, entity.name, parentEntry.common)
    }

    private fun deletePackage(entity: IPackage): DeletePackage? {
        val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IPackage) not found on LUT.")
            return null
        }
        logger.debug("$entity(IPackage)")
        entityTable.entries.remove(entry)
        return DeletePackage(entry.common)
    }

    companion object : Logging {
        private val logger = logger()
    }
}