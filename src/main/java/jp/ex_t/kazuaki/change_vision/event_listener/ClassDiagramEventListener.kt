/*
 * ClassDiagramEventListener.kt - pair-modeling-prototype
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

class ClassDiagramEventListener(private val mqttPublisher: MqttPublisher) : IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        val removeOperations = removeProjectEditUnit.mapNotNull { editUnit ->
            when (val entity = editUnit.entity) {
                is IClass -> deleteClassModel(entity)
                is IAssociation -> deleteAssociationModel(entity, removeProjectEditUnit) ?: return
                is IGeneralization -> deleteGeneralizationModel(entity, removeProjectEditUnit) ?: return
                is IRealization -> deleteRealizationModel(entity, removeProjectEditUnit) ?: return
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
                is INodePresentation -> createClassPresentation(entity)
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
                is INodePresentation -> resizeClassPresentation(entity)
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

    private fun deleteClassModel(entity: IClass): DeleteClassModel {
        val api = AstahAPI.getAstahAPI()
        val brotherClassNameList = api.projectAccessor.findElements(IClass::class.java)
            .filterNot { it == entity }.map { it?.name }.toList()
        logger.debug("${entity.name}(IClass)")
        return DeleteClassModel(brotherClassNameList)
    }

    private fun deleteAssociationModel(
        entity: IAssociation,
        removeProjectEditUnit: List<ProjectEditUnit>
    ): DeleteLinkModel? {
        // TODO: 関連モデルだけで削除された場合に、どの関連モデルが削除されたか認識できるようにする
        return if (removeProjectEditUnit.any { it.entity is ILinkPresentation && (it.entity as ILinkPresentation).model is IAssociation }) {
            logger.debug("${entity.name}(IAssociation")
            DeleteLinkModel(true)
        } else {
            logger.debug("$entity(IAssociation)")
            logger.warn("This operation does not support because plugin can't detect what association model delete.")
            null
        }
    }

    private fun deleteGeneralizationModel(
        entity: IGeneralization,
        removeProjectEditUnit: List<ProjectEditUnit>
    ): DeleteLinkModel? {
        // TODO: 汎化モデルだけで削除された場合に、どの汎化モデルが削除されたか認識できるようにする
        return if (removeProjectEditUnit.any { it.entity is ILinkPresentation && (it.entity as ILinkPresentation).model is IGeneralization }) {
            logger.debug("${entity.name}(IGeneralization")
            DeleteLinkModel(true)
        } else {
            logger.debug("$entity(IGeneralization)")
            logger.warn("This operation does not support because plugin can't detect what generalization model delete.")
            null
        }
    }

    private fun deleteRealizationModel( //
        entity: IRealization,
        removeProjectEditUnit: List<ProjectEditUnit>
    ): DeleteLinkModel? {
        // TODO: 実現モデルだけで削除された場合に、どの実現モデルが削除されたか認識できるようにする
        return if (removeProjectEditUnit.any { it.entity is ILinkPresentation && (it.entity as ILinkPresentation).model is IRealization }) {
            logger.debug("${entity.name}(IRealization")
            DeleteLinkModel(true)
        } else {
            logger.debug("$entity(IRealization)")
            logger.warn("This operation does not support because plugin can't detect what realization model delete.")
            null
        }
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
                val serializablePoints = entity.points.map { point -> Pair(point.x, point.y) }.toList()
                logger.debug("${entity.label}(ILinkPresentation, IAssociation)")
                DeleteLinkPresentation(serializablePoints, "Association")
            }
            is IGeneralization -> {
                val serializablePoints = entity.points.map { point -> Pair(point.x, point.y) }.toList()
                logger.debug("${model.name}(ILinkPresentation, IGeneralization)")
                DeleteLinkPresentation(serializablePoints, "Generalization")
            }
            is IRealization -> {
                val serializablePoints = entity.points.map { point -> Pair(point.x, point.y) }.toList()
                logger.debug("${model.name}(ILinkPresentation, IRealization)")
                DeleteLinkPresentation(serializablePoints, "Realization")
            }
            else -> {
                logger.debug("$entity(ILinkPresentation)")
                null
            }
        }
    }

    private fun deleteNodePresentation(entity: INodePresentation): DeleteClassPresentation? {
        return when (val model = entity.model) {
            is IClass -> {
                logger.debug("${model.name}(INodePresentation, IClass)")
                DeleteClassPresentation(model.name)
            }
            else -> {
                logger.debug("${model}(INodePresentation, Unknown)")
                null
            }
        }
    }

    private fun createClassDiagram(entity: IClassDiagram): ClassDiagramOperation {
        val owner = entity.owner as INamedElement
        val createClassDiagram = CreateClassDiagram(entity.name, owner.name)
        logger.debug("${entity.name}(IClassDiagram)")
        return createClassDiagram
    }

    private fun createClassModel(entity: IClass): CreateClassModel {
        val parentPackage = entity.owner as IPackage
        val createClassModel = CreateClassModel(entity.name, parentPackage.name, entity.stereotypes.toList())
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
                        val sourceClassNavigability = entity.memberEnds.first().navigability
                        val destinationClassNavigability = entity.memberEnds.last().navigability
                        logger.debug("${sourceClass.name}(IClass, $sourceClassNavigability) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass, $destinationClassNavigability)")
                        CreateAssociationModel(
                            sourceClass.name,
                            sourceClassNavigability,
                            destinationClass.name,
                            destinationClassNavigability,
                            entity.name,
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

    private fun createGeneralizationModel(entity: IGeneralization): ClassDiagramOperation {
        val superClass = entity.superType
        val subClass = entity.subType
        logger.debug("${superClass.name}(IClass) -> ${entity.name}(IGeneralization) - ${subClass.name}(IClass)")
        return CreateGeneralizationModel(superClass.name, subClass.name, entity.name)
    }

    private fun createRealizationModel(entity: IRealization): ClassDiagramOperation {
        val supplierClass = entity.supplier
        val clientClass = entity.client
        logger.debug("${supplierClass.name}(IClass) -> ${entity.name}(IRealization) -> ${clientClass.name}(IClass)")
        return CreateRealizationModel(supplierClass.name, clientClass.name, entity.name)
    }

    private fun createOperation(entity: IOperation): CreateOperation? {
        return when (val owner = entity.owner) {
            is IClass -> {
                logger.debug("${entity.name}(IOperation) - $owner(IClass)")
                CreateOperation(owner.name, entity.name, entity.returnTypeExpression)
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
                logger.debug("${entity.name}(IAttribute) - ${owner}(IClass)")
                CreateAttribute(owner.name, entity.name, entity.typeExpression)
            }
            else -> {
                logger.debug("${entity.name}(IAttribute) - ${owner}(Unknown)")
                null
            }
        }
    }

    private fun createClassPresentation(entity: INodePresentation): CreateClassPresentation? {
        return when (val diagram = entity.diagram) {
            is IClassDiagram -> {
                when (val model = entity.model) {
                    is IClass -> {
                        // TODO: クラスとインタフェースをINodePresentationのプロパティで見分ける
                        val location = Pair(entity.location.x, entity.location.y)
                        logger.debug(
                            "${entity.label}(INodePresentation)::${model.name}(IClass, ${
                                Pair(
                                    entity.width,
                                    entity.height
                                )
                            } at ${entity.location})"
                        )
                        CreateClassPresentation(model.name, location, diagram.name)
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
                        when (entity.model) {
                            is IAssociation -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IAssociation) - ${target.name}(IClass)")
                                CreateLinkPresentation(source.name, target.name, "Association", entity.diagram.name)
                            }
                            is IGeneralization -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IGeneralization) - ${target.name}(IClass)")
                                CreateLinkPresentation(source.name, target.name, "Generalization", entity.diagram.name)
                            }
                            is IRealization -> {
                                logger.debug("${source.name}(IClass, interface) - ${entity.label}(ILinkPresentation::IRealization) - ${target.name}(IClass)")
                                CreateLinkPresentation(source.name, target.name, "Realization", entity.diagram.name)
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

    private fun changeClassModel(entity: IClass): ChangeClassModel {
        val api = AstahAPI.getAstahAPI()
        val brotherClassNameList = api.projectAccessor.findElements(IClass::class.java)
            .filterNot { it.name == entity.name }.map { it?.name }.toList()
        logger.debug("${entity.name}(IClass) which maybe new name has ${entity.stereotypes.toList()} stereotype")
        return ChangeClassModel(entity.name, brotherClassNameList, entity.stereotypes.toList())
    }

    private fun resizeClassPresentation(entity: INodePresentation): ResizeClassPresentation? {
        return when (val diagram = entity.diagram) {
            is IClassDiagram -> {
                when (val model = entity.model) {
                    is IClass -> {
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
                        ResizeClassPresentation(model.name, location, size, diagram.name)
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
                val brotherNameAndReturnTypeExpression = owner.operations.filterNot { it == entity }
                    .map { Pair(it.name, it.returnTypeExpression) }.toList()
                logger.debug("${entity.name}:${entity.returnTypeExpression}/${entity.returnType}(IOperation) - ${entity.owner}(IClass)")
                ChangeOperationNameAndReturnTypeExpression(
                    owner.name,
                    brotherNameAndReturnTypeExpression,
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
                val brotherNameAndTypeExpression =
                    owner.attributes.filterNot { it == entity }.map { Pair(it.name, it.typeExpression) }.toList()
                logger.debug("${entity.name}:${entity.typeExpression}/${entity.type}(IAttribute) - ${entity.owner}(IClass)")
                ChangeAttributeNameAndTypeExpression(
                    owner.name,
                    brotherNameAndTypeExpression,
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