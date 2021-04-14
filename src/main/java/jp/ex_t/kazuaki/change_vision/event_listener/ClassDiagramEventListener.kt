/*
 * ClassDiagramEventListener.kt - pair-modeling-prototype
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

class ClassDiagramEventListener(private val entityLUT: EntityLUT, private val mqttPublisher: MqttPublisher) :
    IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        val removeOperations = removeProjectEditUnit.mapNotNull { editUnit ->
            Operation.values()[editUnit.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = editUnit.entity) {
                is IClass -> deleteClassModel(entity)
                is IAssociation -> deleteAssociationModel(entity) ?: return
                is IGeneralization -> deleteGeneralizationModel(entity) ?: return
                is IRealization -> deleteRealizationModel(entity) ?: return
                is ILinkPresentation -> deleteLinkPresentation(entity, removeProjectEditUnit)
                is INodePresentation -> deleteNodePresentation(entity)

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
        val addDiagramUnit = addProjectEditUnit.filter { it.entity is IClassDiagram }
        addDiagramUnit.forEach {
            val createDiagramTransaction = Transaction(listOf(createClassDiagram(it.entity as IClassDiagram)))
            ProjectChangedListener.encodeAndPublish(createDiagramTransaction, mqttPublisher)
            return
        }

        val otherAddUnit = addProjectEditUnit - addDiagramUnit
        val createOperations = otherAddUnit.mapNotNull {
            Operation.values()[it.operation].let { op -> logger.debug("Op: $op -> ") }
            when (val entity = it.entity) {
                is IClass -> createClassModel(entity)
                is IAssociation -> createAssociationModel(entity)
                is IGeneralization -> createGeneralizationModel(entity)
                is IRealization -> createRealizationModel(entity)
                is IOperation -> createOperation(entity)
                is IAttribute -> createAttribute(entity)
                is IComment -> createComment(entity)
                is INodePresentation -> createNodePresentation(entity)
                is ILinkPresentation -> createLinkPresentation(entity)
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
                is IClass -> changeClassModel(entity)
                is INodePresentation -> resizeNodePresentation(entity)
                is IOperation -> changeOperationNameAndReturnTypeExpression(entity) // break
                is IAttribute -> changeAttributeNameAndTypeExpression(entity) // break
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

    private fun createComment(entity: IComment): ClassDiagramOperation? {
        logger.debug("${entity}(IComment)")
        return null
    }

    private fun deleteClassModel(entity: IClass): DeleteClassModel? {
        val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IClass) not found on LUT.")
            return null
        }
        entityLUT.entries.remove(lutEntity)
        logger.debug("${entity.name}(IClass)")
        return DeleteClassModel(lutEntity.common)
    }

    private fun deleteAssociationModel(
        entity: IAssociation,
    ): DeleteLinkModel? {
        val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IAssociation) not found on LUT.")
            return null
        }
        entityLUT.entries.remove(lutEntity)
        logger.debug("${entity.name}(IAssociation)")
        return DeleteLinkModel(lutEntity.common)
    }

    private fun deleteGeneralizationModel(
        entity: IGeneralization,
    ): DeleteLinkModel? {
        val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IGeneralization) not found on LUT.")
            return null
        }
        entityLUT.entries.remove(lutEntity)
        logger.debug("${entity.name}(IGeneralization)")
        return DeleteLinkModel(lutEntity.common)
    }

    private fun deleteRealizationModel(
        entity: IRealization,
    ): DeleteLinkModel? {
        val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IRealization) not found on LUT.")
            return null
        }
        entityLUT.entries.remove(lutEntity)
        logger.debug("${entity.name}(IRealization)")
        return DeleteLinkModel(lutEntity.common)
    }

    private fun deleteLinkPresentation(
        entity: ILinkPresentation,
        removeProjectEditUnit: List<ProjectEditUnit>
    ): DeleteLinkPresentation? {
        if (removeProjectEditUnit.any { it.entity is IClass }) {
            return null
        }
        return when (val model = entity.model) {
            is IAssociation -> {
                val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(ILinkPresentation, IAssociation) not found on LUT.")
                    return null
                }
                entityLUT.entries.remove(lutEntity)
                logger.debug("${entity.label}(ILinkPresentation, IAssociation)")
                DeleteLinkPresentation(lutEntity.common)
            }
            is IGeneralization -> {
                val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(ILinkPresentation, IGeneralization) not found on LUT.")
                    return null
                }
                entityLUT.entries.remove(lutEntity)
                logger.debug("${model.name}(ILinkPresentation, IGeneralization)")
                DeleteLinkPresentation(lutEntity.common)
            }
            is IRealization -> {
                val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(ILinkPresentation, IRealization) not found on LUT.")
                    return null
                }
                entityLUT.entries.remove(lutEntity)
                logger.debug("${model.name}(ILinkPresentation, IRealization)")
                DeleteLinkPresentation(lutEntity.common)
            }
            else -> {
                logger.debug("$entity(ILinkPresentation)")
                null
            }
        }
    }

    private fun deleteNodePresentation(entity: INodePresentation): ClassDiagramOperation? {
        return when (val model = entity.model) {
            is IClass -> {
                val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(INodePresentation, IClass) not found on LUT.")
                    return null
                }
                entityLUT.entries.remove(lutEntity)
                logger.debug("${model.name}(INodePresentation, IClass)")
                DeleteClassPresentation(lutEntity.common)
            }
            is IComment -> {
                val lutEntity = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(INodePresentation, IComment) not found on LUT.")
                    return null
                }
                entityLUT.entries.remove(lutEntity)
                logger.debug("${model.name}(INodePresentation, IComment)")
                DeleteNote(lutEntity.common)
            }
            else -> {
                logger.debug("${model}(INodePresentation, Unknown)")
                null
            }
        }
    }

    private fun createClassDiagram(entity: IClassDiagram): ClassDiagramOperation {
        val owner = entity.owner as INamedElement
        // TODO: ownerのIDをどうにかして共有させる
        val createClassDiagram = CreateClassDiagram(entity.name, owner.name, entity.id)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("${entity.name}(IClassDiagram)")
        return createClassDiagram
    }

    private fun createClassModel(entity: IClass): CreateClassModel {
        entityLUT.entries.add(Entry(entity.id, entity.id))
//        val parentEntry = entityLUT.entries.find { it.mine == entity.owner.id } ?: run {
//            logger.debug("${entity.owner.id} not found on LUT.")
//            return null
//        }
        // TODO: ownerのIDをLUTに登録できるようにする(最初はパッケージのIDを登録していないので取れない)
        val owner = entity.owner as INamedElement
        val createClassModel = CreateClassModel(entity.name, owner.name, entity.stereotypes.toList(), entity.id)
        logger.debug("${entity.name}(IClass)")
        return createClassModel
    }

    private fun createAssociationModel(entity: IAssociation): CreateAssociationModel? {
        val sourceClass = entity.memberEnds.first().owner
        val destinationClass = entity.memberEnds.last().owner
        return when (sourceClass) {
            is IClass -> {
                when (destinationClass) {
                    is IClass -> {
                        val sourceClassEntry = entityLUT.entries.find { it.mine == sourceClass.id } ?: run {
                            logger.debug("${sourceClass.id} not found on LUT.")
                            return null
                        }
                        val destinationClassEntry = entityLUT.entries.find { it.mine == destinationClass.id } ?: run {
                            logger.debug("${destinationClass.id} not found on LUT.")
                            return null
                        }
                        val sourceClassNavigability = entity.memberEnds.first().navigability
                        val destinationClassNavigability = entity.memberEnds.last().navigability
                        logger.debug("${sourceClass.name}(IClass, $sourceClassNavigability) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass, $destinationClassNavigability)")
                        entityLUT.entries.add(Entry(entity.id, entity.id))
                        CreateAssociationModel(
                            sourceClassEntry.common,
                            sourceClassNavigability,
                            destinationClassEntry.common,
                            destinationClassNavigability,
                            entity.name,
                            entity.id,
                        )
                    }
                    else -> {
                        logger.debug("${sourceClass.name}(IClass) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                        null
                    }
                }
            }
            else -> {
                logger.debug("$sourceClass(Unknown) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                null
            }
        }
    }

    private fun createGeneralizationModel(entity: IGeneralization): ClassDiagramOperation? {
        val superClass = entity.superType
        val subClass = entity.subType
        val superClassEntry = entityLUT.entries.find { it.mine == superClass.id } ?: run {
            logger.debug("${superClass.id} not found on LUT.")
            return null
        }
        val subClassEntry = entityLUT.entries.find { it.mine == subClass.id } ?: run {
            logger.debug("${subClass.id} not found on LUT.")
            return null
        }
        logger.debug("${superClass.name}(IClass) -> ${entity.name}(IGeneralization) - ${subClass.name}(IClass)")
        entityLUT.entries.add(Entry(entity.id, entity.id))
        return CreateGeneralizationModel(superClassEntry.common, subClassEntry.common, entity.name, entity.id)
    }

    private fun createRealizationModel(entity: IRealization): ClassDiagramOperation? {
        val supplierClass = entity.supplier
        val clientClass = entity.client
        val supplierClassEntry = entityLUT.entries.find { it.mine == supplierClass.id } ?: run {
            logger.debug("${supplierClass.id} not found on LUT.")
            return null
        }
        val clientClassEntry = entityLUT.entries.find { it.mine == clientClass.id } ?: run {
            logger.debug("${clientClass.id} not found on LUT.")
            return null
        }
        logger.debug("${supplierClass.name}(IClass) -> ${entity.name}(IRealization) -> ${clientClass.name}(IClass)")
        entityLUT.entries.add(Entry(entity.id, entity.id))
        return CreateRealizationModel(supplierClassEntry.common, clientClassEntry.common, entity.name, entity.id)
    }

    private fun createOperation(entity: IOperation): CreateOperation? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}(IOperation) - $owner(IClass)")
                entityLUT.entries.add(Entry(entity.id, entity.id))
                CreateOperation(ownerEntry.common, entity.name, entity.returnTypeExpression, entity.id)
            }
            else -> {
                logger.debug("${entity.name}(IOperation) - $owner(Unknown)")
                null
            }
        }
    }

    private fun createAttribute(entity: IAttribute): CreateAttribute? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}(IAttribute) - ${owner}(IClass)")
                entityLUT.entries.add(Entry(entity.id, entity.id))
                CreateAttribute(ownerEntry.common, entity.name, entity.typeExpression, entity.id)
            }
            else -> {
                logger.debug("${entity.name}(IAttribute) - ${owner}(Unknown)")
                null
            }
        }
    }

    private fun createNodePresentation(entity: INodePresentation): ClassDiagramOperation? {
        return when (val diagram = entity.diagram) {
            is IClassDiagram -> {
                when (val model = entity.model) {
                    is IClass -> {
                        val classModelEntry = entityLUT.entries.find { it.mine == model.id } ?: run {
                            logger.debug("${model.id} not found on LUT.")
                            return null
                        }
                        val location = Pair(entity.location.x, entity.location.y)
                        logger.debug(
                            "${entity.label}(INodePresentation)::${model.name}(IClass, ${
                                Pair(
                                    entity.width,
                                    entity.height
                                )
                            } at ${entity.location})"
                        )
                        entityLUT.entries.add(Entry(entity.id, entity.id))
                        CreateClassPresentation(classModelEntry.common, location, diagram.name, entity.id)
                    }
                    is IComment -> {
                        val location = Pair(entity.location.x, entity.location.y)
                        val size = Pair(entity.width, entity.height)
                        logger.debug("${entity.label}(INodePresentation) - $model(IComment)")
                        entityLUT.entries.add(Entry(entity.id, entity.id))
                        CreateNotePresentation(entity.label, location, size, entity.diagram.name, entity.id)
                    }
                    else -> {
                        logger.debug("${entity.label}(INodePresentation) - $model(Unknown)")
                        null
                    }
                }
            }
            else -> {
                logger.debug("${entity.label}(Unknown)")
                null
            }
        }
    }

    private fun createLinkPresentation(entity: ILinkPresentation): CreateLinkPresentation? {
        val source = entity.source.model
        val target = entity.target.model
        logger.debug("Model: ${entity.model::class.java}")
        return when (source) {
            is IClass -> {
                when (target) {
                    is IClass -> {
                        val sourceEntry = entityLUT.entries.find { it.mine == source.id } ?: run {
                            logger.debug("${source.id} not found on LUT.")
                            return null
                        }
                        val targetEntry = entityLUT.entries.find { it.mine == target.id } ?: run {
                            logger.debug("${target.id} not found on LUT.")
                            return null
                        }
                        when (entity.model) {
                            is IAssociation -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IAssociation) - ${target.name}(IClass)")
                                entityLUT.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    sourceEntry.common,
                                    targetEntry.common,
                                    "Association",
                                    entity.diagram.name,
                                    entity.id
                                )
                            }
                            is IGeneralization -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IGeneralization) - ${target.name}(IClass)")
                                entityLUT.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    sourceEntry.common,
                                    targetEntry.common,
                                    "Generalization",
                                    entity.diagram.name,
                                    entity.id
                                )
                            }
                            is IRealization -> {
                                logger.debug("${source.name}(IClass, interface) - ${entity.label}(ILinkPresentation::IRealization) - ${target.name}(IClass)")
                                entityLUT.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    sourceEntry.common,
                                    targetEntry.common,
                                    "Realization",
                                    entity.diagram.name,
                                    entity.id
                                )
                            }
                            else -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::Unknown) - ${target.name}(IClass)")
                                null
                            }
                        }
                    }
                    else -> {
                        logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                        null
                    }
                }
            }
            else -> {
                logger.debug("$source(Unknown) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                null
            }
        }
    }

    private fun changeClassModel(entity: IClass): ChangeClassModel? {
        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            return null
        }
        logger.debug("${entity.name}(IClass) which maybe new name has ${entity.stereotypes.toList()} stereotype")
        return ChangeClassModel(entry.common, entity.name, entity.stereotypes.toList())
    }

    private fun resizeNodePresentation(entity: INodePresentation): ClassDiagramOperation? {
        return when (val diagram = entity.diagram) {
            is IClassDiagram -> {
                when (val model = entity.model) {
                    is IClass -> {
                        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
                            logger.debug("${entity.id} not found on LUT.")
                            return null
                        }
                        val location = Pair(entity.location.x, entity.location.y)
                        val size = Pair(entity.width, entity.height)
                        logger.debug(
                            "${entity.label}(INodePresentation)::${model.name}(IClass, ${
                                Pair(
                                    entity.width,
                                    entity.height
                                )
                            } at ${entity.location}) @ClassDiagram${diagram.name}"
                        )
                        ResizeClassPresentation(entry.common, location, size, diagram.name)
                    }
                    is IComment -> {
                        val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
                            logger.debug("${entity.id} not found on LUT.")
                            return null
                        }
                        val location = Pair(entity.location.x, entity.location.y)
                        val size = Pair(entity.width, entity.height)
                        logger.debug(
                            "${entity.label}(INodePresentation)::${model.name}(IComment, ${
                                Pair(
                                    entity.width,
                                    entity.height
                                )
                            } at ${entity.location}) @ClassDiagram${diagram.name}"
                        )
                        ResizeNote(entry.common, entity.label, location, size, diagram.name)
                    }
                    else -> {
                        logger.debug("${entity.label}(INodePresentation) - $model(Unknown)")
                        null
                    }
                }
            }
            else -> {
                logger.debug("${entity.label}(INodePresentation) @UnknownDiagram")
                null
            }
        }
    }

    private fun changeOperationNameAndReturnTypeExpression(entity: IOperation): ChangeOperationNameAndReturnTypeExpression? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}:${entity.returnTypeExpression}/${entity.returnType}(IOperation) - ${entity.owner}(IClass)")
                ChangeOperationNameAndReturnTypeExpression(
                    ownerEntry.common,
                    entry.common,
                    entity.name,
                    entity.returnTypeExpression
                )
            }
            else -> {
                logger.debug("$entity(IOperation) - ${entity.owner}(Unknown)")
                null
            }
        }
    }

    private fun changeAttributeNameAndTypeExpression(entity: IAttribute): ChangeAttributeNameAndTypeExpression? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                val entry = entityLUT.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}:${entity.typeExpression}/${entity.type}(IAttribute) - ${entity.owner}(IClass)")
                ChangeAttributeNameAndTypeExpression(
                    ownerEntry.common,
                    entry.common,
                    entity.name,
                    entity.typeExpression
                )
            }
            else -> {
                logger.debug("$entity(IAttribute) - ${entity.owner}(Unknown)")
                null
            }
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}