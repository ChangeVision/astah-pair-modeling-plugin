/*
 * StateMachineDiagramEventListener.java - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */
package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectEditUnit
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi

class StateMachineDiagramEventListener(private val entityLUT: EntityLUT, private val mqttPublisher: MqttPublisher) :
    IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        val removeOperations = removeProjectEditUnit.mapNotNull { editUnit ->
            Operation.values()[editUnit.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = editUnit.entity) {
                is INodePresentation -> {
                    when (entity.model) {
                        is IPseudostate -> deletePseudostate(entity)
                        is IFinalState -> deleteFinalState(entity)
                        is IState -> deleteState(entity)
                        else -> {
                            logger.debug("$entity(INodePresentation, Unknown)")
                            null
                        }
                    }
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

        val addProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        val addDiagramUnit = addProjectEditUnit.filter { it.entity is IStateMachineDiagram }
        addDiagramUnit.forEach {
            val createDiagramTransaction =
                Transaction(listOf(createStateMachineDiagram(it.entity as IStateMachineDiagram)))
            ProjectChangedListener.encodeAndPublish(createDiagramTransaction, mqttPublisher)
            return
        }

        val otherAddUnit = addProjectEditUnit - addDiagramUnit
        val createOperations = otherAddUnit.mapNotNull {
            Operation.values()[it.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = it.entity) {
                is INodePresentation -> {
                    when (entity.model) {
                        is IPseudostate -> createPseudostate(entity)
                        is IFinalState -> createFinalState(entity)
                        is IState -> createState(entity)
                        else -> {
                            logger.debug("$entity(INodePresentation, Unknown)")
                            null
                        }
                    }
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
        val modifyOperations = modifyProjectEditUnit.mapNotNull {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is INodePresentation -> {
                    when (entity.model) {
                        is IPseudostate -> modifyPseudostate(entity)
                        is IFinalState -> modifyFinalState(entity)
                        is IState -> modifyState(entity)
                        else -> {
                            logger.debug("$entity(INodePresentation, Unknown)")
                            null
                        }
                    }
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

    private fun createStateMachineDiagram(entity: IStateMachineDiagram): CreateStateMachineDiagram {
        val owner = entity.owner as INamedElement
        val createStateMachineDiagram = CreateStateMachineDiagram(entity.name, owner.name, entity.id)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("$entity(IStateMachineDiagram)")
        return createStateMachineDiagram
    }

    private fun createPseudostate(entity: INodePresentation): CreatePseudostate? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.id}(INodePresentation, IPseudostate) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("$entity(INodePresentation, IPseudostate)")
        return CreatePseudostate(entity.id, location, size, parentEntry.common)
    }

    private fun createState(entity: INodePresentation): CreateState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.id}(INodePresentation, IState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("$entity(INodePresentation, IState)")
        return CreateState(entity.id, entity.label, location, size, parentEntry.common)
    }

    private fun createFinalState(entity: INodePresentation): CreateFinalState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.id}(INodePresentation, IFinalState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("$entity(INodePresentation, IFinalState)")
        return CreateFinalState(entity.id, location, size, parentEntry.common)
    }

    private fun modifyPseudostate(entity: INodePresentation): ModifyPseudostate? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.id}(Parent of INodePresentation, IPseudostate) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INodePresentation, IPseudostate) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IPseudostate)")
        return ModifyPseudostate(entry.common, location, size, parentEntry.common)
    }

    private fun modifyState(entity: INodePresentation): ModifyState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.id}(Parent of INodePresentation, IState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INodePresentation, IState) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IState)")
        return ModifyState(entry.common, entity.label, location, size, parentEntry.common)
    }

    private fun modifyFinalState(entity: INodePresentation): ModifyFinalState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.id}(Parent of INodePresentation, IFinalState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INodePresentation, IFinalState) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IFinalState)")
        return ModifyFinalState(entry.common, location, size, parentEntry.common)
    }

    private fun deletePseudostate(entity: INodePresentation): DeletePseudostate? {
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INodePresentation, IPseudostate) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IPseudostate)")
        entityLUT.entries.remove(entry)
        return DeletePseudostate(entry.common)
    }

    private fun deleteState(entity: INodePresentation): DeleteState? {
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INodePresentation, IState) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IState)")
        entityLUT.entries.remove(entry)
        return DeleteState(entry.common)
    }

    private fun deleteFinalState(entity: INodePresentation): DeleteFinalState? {
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INodePresentation, IFinalState) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IFinalState)")
        entityLUT.entries.remove(entry)
        return DeleteFinalState(entry.common)
    }

    companion object : Logging {
        private val logger = logger()
    }
}