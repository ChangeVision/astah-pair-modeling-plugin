/*
 * ClassDiagramEventListener.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectEditUnit
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi

class ClassDiagramEventListener(private val entityTable: EntityTable, private val mqttPublisher: MqttPublisher) :
    IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        val removeDiagramUnit = removeProjectEditUnit.filter { it.entity is IClassDiagram }
        val removeDiagramOperation = removeDiagramUnit.mapNotNull { deleteClassDiagram(it.entity as IClassDiagram) }
        if (removeDiagramOperation.isNotEmpty()) {
            ProjectChangedListener.encodeAndPublish(Transaction(removeDiagramOperation), mqttPublisher)
            return
        }

        val otherRemoveUnit = removeProjectEditUnit - removeDiagramUnit
        val removeOperations = otherRemoveUnit.mapNotNull { editUnit ->
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
                is IClassDiagram -> modifyClassDiagram(entity)
                is IClass -> modifyClassModel(entity)
                is INodePresentation -> modifyNodePresentation(entity)
                is IOperation -> modifyOperation(entity) // break
                is IAttribute -> modifyAttribute(entity) // break
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

    private fun deleteClassDiagram(entity: IClassDiagram): DeleteClassDiagram? {
        return run {
            val lut = entityTable.entries.find { it.mine == entity.id } ?: run {
                logger.debug("${entity.id} not found on LUT.")
                return null
            }
            entityTable.entries.remove(lut)
            logger.debug("${entity}(IClassDiagram)")
            DeleteClassDiagram(lut.common)
        }
    }

    private fun createComment(entity: IComment): ClassDiagramOperation? {
        logger.debug("${entity}(IComment)")
        return null
    }

    private fun deleteClassModel(entity: IClass): DeleteModel? {
        val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IClass) not found on LUT.")
            return null
        }
        entityTable.entries.remove(lutEntity)
        logger.debug("${entity.name}(IClass)")
        return DeleteModel(lutEntity.common)
    }

    private fun deleteAssociationModel(
        entity: IAssociation,
    ): DeleteModel? {
        val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IAssociation) not found on LUT.")
            return null
        }
        entityTable.entries.remove(lutEntity)
        logger.debug("${entity.name}(IAssociation)")
        return DeleteModel(lutEntity.common)
    }

    private fun deleteGeneralizationModel(
        entity: IGeneralization,
    ): DeleteModel? {
        val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IGeneralization) not found on LUT.")
            return null
        }
        entityTable.entries.remove(lutEntity)
        logger.debug("${entity.name}(IGeneralization)")
        return DeleteModel(lutEntity.common)
    }

    private fun deleteRealizationModel(
        entity: IRealization,
    ): DeleteModel? {
        val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id}(IRealization) not found on LUT.")
            return null
        }
        entityTable.entries.remove(lutEntity)
        logger.debug("${entity.name}(IRealization)")
        return DeleteModel(lutEntity.common)
    }

    private fun deleteLinkPresentation(
        entity: ILinkPresentation,
        removeProjectEditUnit: List<ProjectEditUnit>
    ): DeletePresentation? {
        if (removeProjectEditUnit.any { it.entity is IClass }) {
            return null
        }
        val diagramEntry = entityTable.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return when (val model = entity.model) {
            is IAssociation -> {
                val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(ILinkPresentation, IAssociation) not found on LUT.")
                    return null
                }
                entityTable.entries.remove(lutEntity)
                logger.debug("${entity.label}(ILinkPresentation, IAssociation)")
                DeletePresentation(lutEntity.common, diagramEntry.common)
            }
            is IGeneralization -> {
                val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(ILinkPresentation, IGeneralization) not found on LUT.")
                    return null
                }
                entityTable.entries.remove(lutEntity)
                logger.debug("${model.name}(ILinkPresentation, IGeneralization)")
                DeletePresentation(lutEntity.common, diagramEntry.common)
            }
            is IRealization -> {
                val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(ILinkPresentation, IRealization) not found on LUT.")
                    return null
                }
                entityTable.entries.remove(lutEntity)
                logger.debug("${model.name}(ILinkPresentation, IRealization)")
                DeletePresentation(lutEntity.common, diagramEntry.common)
            }
            else -> {
                logger.debug("$entity(ILinkPresentation)")
                null
            }
        }
    }

    private fun deleteNodePresentation(entity: INodePresentation): ClassDiagramOperation? {
        val diagram = AstahAPI.getAstahAPI().viewManager.diagramViewManager.currentDiagram
        val diagramEntry = entityTable.entries.find { it.mine == diagram.id } ?: run {
            logger.debug("${diagram.id} not found on LUT.")
            return null
        }
        return when (val model = entity.model) {
            is IClass -> {
                val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(INodePresentation, IClass) not found on LUT.")
                    return null
                }
                entityTable.entries.remove(lutEntity)
                logger.debug("${model.name}(INodePresentation, IClass)")
                DeletePresentation(lutEntity.common, diagramEntry.common)
            }
            is IComment -> {
                val lutEntity = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id}(INodePresentation, IComment) not found on LUT.")
                    return null
                }
                entityTable.entries.remove(lutEntity)
                logger.debug("${model.name}(INodePresentation, IComment)")
                DeletePresentation(lutEntity.common, diagramEntry.common)
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
        entityTable.entries.add(Entry(entity.id, entity.id))
        logger.debug("${entity.name}(IClassDiagram)")
        return createClassDiagram
    }

    private fun createClassModel(entity: IClass): CreateClassModel {
        entityTable.entries.add(Entry(entity.id, entity.id))
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
                        val sourceClassEntry = entityTable.entries.find { it.mine == sourceClass.id } ?: run {
                            logger.debug("${sourceClass.id} not found on LUT.")
                            return null
                        }
                        val destinationClassEntry = entityTable.entries.find { it.mine == destinationClass.id } ?: run {
                            logger.debug("${destinationClass.id} not found on LUT.")
                            return null
                        }
                        val sourceClassNavigability = entity.memberEnds.first().navigability
                        val destinationClassNavigability = entity.memberEnds.last().navigability
                        logger.debug("${sourceClass.name}(IClass, $sourceClassNavigability) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass, $destinationClassNavigability)")
                        entityTable.entries.add(Entry(entity.id, entity.id))
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
        val superClassEntry = entityTable.entries.find { it.mine == superClass.id } ?: run {
            logger.debug("${superClass.id} not found on LUT.")
            return null
        }
        val subClassEntry = entityTable.entries.find { it.mine == subClass.id } ?: run {
            logger.debug("${subClass.id} not found on LUT.")
            return null
        }
        logger.debug("${superClass.name}(IClass) -> ${entity.name}(IGeneralization) - ${subClass.name}(IClass)")
        entityTable.entries.add(Entry(entity.id, entity.id))
        return CreateGeneralizationModel(superClassEntry.common, subClassEntry.common, entity.name, entity.id)
    }

    private fun createRealizationModel(entity: IRealization): ClassDiagramOperation? {
        val supplierClass = entity.supplier
        val clientClass = entity.client
        val supplierClassEntry = entityTable.entries.find { it.mine == supplierClass.id } ?: run {
            logger.debug("${supplierClass.id} not found on LUT.")
            return null
        }
        val clientClassEntry = entityTable.entries.find { it.mine == clientClass.id } ?: run {
            logger.debug("${clientClass.id} not found on LUT.")
            return null
        }
        logger.debug("${supplierClass.name}(IClass) -> ${entity.name}(IRealization) -> ${clientClass.name}(IClass)")
        entityTable.entries.add(Entry(entity.id, entity.id))
        return CreateRealizationModel(supplierClassEntry.common, clientClassEntry.common, entity.name, entity.id)
    }

    private fun createOperation(entity: IOperation): CreateOperation? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityTable.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}(IOperation) - $owner(IClass)")
                entityTable.entries.add(Entry(entity.id, entity.id))
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
                val ownerEntry = entityTable.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}(IAttribute) - ${owner}(IClass)")
                entityTable.entries.add(Entry(entity.id, entity.id))
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
                        val classModelEntry = entityTable.entries.find { it.mine == model.id } ?: run {
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
                        entityTable.entries.add(Entry(entity.id, entity.id))
                        val diagramEntry = entityTable.entries.find { it.mine == diagram.id } ?: run {
                            logger.debug("${diagram.id} not found on LUT.")
                            return null
                        }
                        CreateClassPresentation(classModelEntry.common, location, diagramEntry.common, entity.id)
                    }
                    is IComment -> {
                        val location = Pair(entity.location.x, entity.location.y)
                        val size = Pair(entity.width, entity.height)
                        logger.debug("${entity.label}(INodePresentation) - $model(IComment)")
                        entityTable.entries.add(Entry(entity.id, entity.id))
                        val diagramEntry = entityTable.entries.find { it.mine == diagram.id } ?: run {
                            logger.debug("${diagram.id} not found on LUT.")
                            return null
                        }
                        CreateNote(entity.label, location, size, diagramEntry.common, entity.id)
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
                        val modelEntry = entityTable.entries.find { it.mine == entity.model.id } ?: run {
                            logger.debug("${entity.model.id} not found on LUT.")
                            return null
                        }
                        val sourceEntry = entityTable.entries.find { it.mine == source.id } ?: run {
                            logger.debug("${source.id} not found on LUT.")
                            return null
                        }
                        val targetEntry = entityTable.entries.find { it.mine == target.id } ?: run {
                            logger.debug("${target.id} not found on LUT.")
                            return null
                        }
                        val diagramEntry = entityTable.entries.find { it.mine == entity.diagram.id } ?: run {
                            logger.debug("${entity.diagram.id} not found on LUT.")
                            return null
                        }
                        when (entity.model) {
                            is IAssociation -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IAssociation) - ${target.name}(IClass)")
                                entityTable.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    modelEntry.common,
                                    sourceEntry.common,
                                    targetEntry.common,
                                    LinkType.Association,
                                    diagramEntry.common,
                                    entity.id
                                )
                            }
                            is IGeneralization -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IGeneralization) - ${target.name}(IClass)")
                                entityTable.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    modelEntry.common,
                                    sourceEntry.common,
                                    targetEntry.common,
                                    LinkType.Generalization,
                                    diagramEntry.common,
                                    entity.id
                                )
                            }
                            is IRealization -> {
                                logger.debug("${source.name}(IClass, interface) - ${entity.label}(ILinkPresentation::IRealization) - ${target.name}(IClass)")
                                entityTable.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    modelEntry.common,
                                    sourceEntry.common,
                                    targetEntry.common,
                                    LinkType.Realization,
                                    diagramEntry.common,
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

    private fun modifyClassDiagram(entity: IClassDiagram): ModifyDiagram? {
        val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            return null
        }
        val ownerEntry = entityTable.entries.find { it.mine == entity.owner.id } ?: run {
            logger.debug("${entity.owner.id} not found on LUT.")
            return null
        }
        logger.debug("${entity.name}(IClassDiagram), owner ${entity.owner}")
        return ModifyDiagram(entity.name, entry.common, ownerEntry.common)
    }

    private fun modifyClassModel(entity: IClass): ModifyClassModel? {
        val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            return null
        }
        logger.debug("${entity.name}(IClass) which maybe new name has ${entity.stereotypes.toList()} stereotype")
        return ModifyClassModel(entry.common, entity.name, entity.stereotypes.toList())
    }

    private fun modifyNodePresentation(entity: INodePresentation): ClassDiagramOperation? {
        return when (val diagram = entity.diagram) {
            is IClassDiagram -> {
                when (val model = entity.model) {
                    is IClass -> modifyClassPresentation(diagram, model, entity)
                    is IComment -> modifyNote(diagram, model, entity)
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

    private fun modifyClassPresentation(
        diagram: IDiagram,
        model: IClass,
        entity: INodePresentation
    ): ModifyClassPresentation? {
        val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
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
        val diagramEntry = entityTable.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return ModifyClassPresentation(entry.common, location, size, diagramEntry.common)
    }

    private fun modifyNote(diagram: IDiagram, model: IComment, entity: INodePresentation): ModifyNote? {
        val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
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
        val diagramEntry = entityTable.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            return null
        }
        return ModifyNote(entry.common, entity.label, location, size, diagramEntry.common)
    }

    private fun modifyOperation(entity: IOperation): ModifyOperation? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityTable.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}:${entity.returnTypeExpression}/${entity.returnType}(IOperation) - ${entity.owner}(IClass)")
                ModifyOperation(
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

    private fun modifyAttribute(entity: IAttribute): ModifyAttribute? {
        return when (val owner = entity.owner) {
            is IClass -> {
                val ownerEntry = entityTable.entries.find { it.mine == owner.id } ?: run {
                    logger.debug("${owner.id} not found on LUT.")
                    return null
                }
                val entry = entityTable.entries.find { it.mine == entity.id } ?: run {
                    logger.debug("${entity.id} not found on LUT.")
                    return null
                }
                logger.debug("${entity.name}:${entity.typeExpression}/${entity.type}(IAttribute) - ${entity.owner}(IClass)")
                ModifyAttribute(
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