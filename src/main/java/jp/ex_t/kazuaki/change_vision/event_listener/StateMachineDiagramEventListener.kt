/*
 * StateMachineDiagramEventListener.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */
package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
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
                is INamedElement -> {
                    deletePresentation(entity)
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
                is ILinkPresentation -> {
                    when (entity.model) {
                        is ITransition -> createTransition(entity)
                        else -> {
                            logger.debug("$entity(ILinkPresentation, Unknown)")
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
                is ILinkPresentation -> {
                    when (entity.model) {
                        is ITransition -> modifyTransition(entity)
                        else -> {
                            logger.debug("$entity(ILinkPresentation, Unknown)")
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
        val entry = Entry(entity.id, entity.id)
        entityLUT.entries.add(entry)
        val createStateMachineDiagram = CreateStateMachineDiagram(entity.name, owner.name, entry.common)
        logger.debug("$entity(IStateMachineDiagram)")
        return createStateMachineDiagram
    }

    private fun createPseudostate(entity: INodePresentation): CreatePseudostate? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IPseudostate) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = Entry(entity.model.id, entity.model.id)
        entityLUT.entries.add(entry)
        logger.debug("$entity(INodePresentation, IPseudostate)")
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return CreatePseudostate(entry.common, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun createState(entity: INodePresentation): CreateState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.model.id}(Parent of INodePresentation, IState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = Entry(entity.model.id, entity.model.id)
        entityLUT.entries.add(entry)
        logger.debug("$entity(INodePresentation, IState)")
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return CreateState(entry.common, entity.label, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun createFinalState(entity: INodePresentation): CreateFinalState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IFinalState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = Entry(entity.model.id, entity.model.id)
        entityLUT.entries.add(entry)
        logger.debug("$entity(INodePresentation, IFinalState)")
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return CreateFinalState(entry.common, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun createTransition(entity: ILinkPresentation): CreateTransition? {
        val sourceEntry = entityLUT.entries.find { it.mine == entity.source.model.id } ?: run {
            logger.debug("${entity.source.model.id}(Source of INodePresentation, IState) not found on LUT.")
            return null
        }

        val targetEntry = entityLUT.entries.find { it.mine == entity.target.model.id } ?: run {
            logger.debug("${entity.model.id}(INodePresentation, IState) not found on LUT.")
            return null
        }

        val entry = Entry(entity.model.id, entity.model.id)
        entityLUT.entries.add(entry)
        logger.debug("$entity(ILinkPresentation, ITransition)")
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return CreateTransition(entry.common, entity.label, sourceEntry.common, targetEntry.common, diagramEntry.common)
    }

    private fun modifyPseudostate(entity: INodePresentation): ModifyPseudostate? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IPseudostate) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = entityLUT.entries.find { it.mine == entity.model.id } ?: run {
            logger.debug("${entity.model.id}(INodePresentation, IPseudostate) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IPseudostate)")
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return ModifyPseudostate(entry.common, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun modifyState(entity: INodePresentation): ModifyState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = entityLUT.entries.find { it.mine == entity.model.id } ?: run {
            logger.debug("${entity.model.id}(INodePresentation, IState) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IState)")
        return ModifyState(entry.common, entity.label, location, size, parentEntry.common)
    }

    private fun modifyFinalState(entity: INodePresentation): ModifyFinalState? {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IFinalState) not found on LUT.")
                    return null
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val entry = entityLUT.entries.find { it.mine == entity.model.id } ?: run {
            logger.debug("${entity.model.id}(INodePresentation, IFinalState) not found on LUT.")
            return null
        }
        logger.debug("$entity(INodePresentation, IFinalState)")
        return ModifyFinalState(entry.common, location, size, parentEntry.common)
    }

    private fun modifyTransition(entity: ILinkPresentation): ModifyTransition? {
        val entry = entityLUT.entries.find { it.mine == entity.model.id } ?: run {
            logger.debug("${entity.model.id}(Parent of ILinkPresentation, ITransition) not found on LUT.")
            return null
        }
        logger.debug("$entity(ILinkPresentation, ITransition)")
        return ModifyTransition(entry.common, entity.label)
    }

    private fun deletePresentation(entity: INamedElement): DeleteModel? {
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(INamedElement) not found on LUT.")
            return null
        }
        logger.debug("$entity(INamedElement)")
        entityLUT.entries.remove(entry)
        return DeleteModel(entry.common)
    }

    companion object : Logging {
        private val logger = logger()
    }
}