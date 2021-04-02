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

class ClassDiagramEventListener(private val mqttPublisher: MqttPublisher): IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val removeTransaction = Transaction()
        val createTransaction = Transaction()
        val modifyTransaction = Transaction()
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        for (it in removeProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is IClass -> {
                    removeTransaction.operations.add(
                        deleteClassModel(entity)
                    )
                }
                is IAssociation -> {
                    removeTransaction.operations.add(
                        deleteAssociationModel(entity, removeProjectEditUnit)
                            ?: continue
                    )
                }
                is IGeneralization -> {
                    removeTransaction.operations.add(
                        deleteGeneralizationModel(entity, removeProjectEditUnit)
                            ?: continue
                    )
                }
                is IRealization -> {
                    removeTransaction.operations.add(
                        deleteRealizationModel(entity, removeProjectEditUnit)
                            ?: continue
                    )
                }
                is ILinkPresentation -> {
                    removeTransaction.operations.add(
                        deleteLinkPresentation(entity, removeProjectEditUnit)
                            ?: continue
                    )
                }
                is INodePresentation -> {
                    removeTransaction.operations.add(
                        deleteNodePresentation(entity)
                            ?: continue
                    )
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                }
            }
        }
        if (removeTransaction.operations.isNotEmpty()) {
            ProjectChangedListener.encodeAndPublish(removeTransaction, mqttPublisher)
            return
        }

        val addProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        for (it in addProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is IClassDiagram -> {
                    val owner = entity.owner as INamedElement
                    val createClassDiagram = CreateClassDiagram(entity.name, owner.name)
                    createTransaction.operations.add(createClassDiagram)
                    logger.debug("${entity.name}(IClassDiagram)")
                    break
                }
                is IClass -> {
                    val parentPackage = entity.owner as IPackage
                    val createClassModel = CreateClassModel(entity.name, parentPackage.name, entity.stereotypes.toList())
                    createTransaction.operations.add(createClassModel)
                    logger.debug("${entity.name}(IClass)")
                }
                is IAssociation -> {
                    val sourceClass = entity.memberEnds.first().owner
                    val destinationClass = entity.memberEnds.last().owner
                    when (sourceClass) {
                        is IClass -> {
                            when (destinationClass) {
                                is IClass -> {
                                    val sourceClassNavigability = entity.memberEnds.first().navigability
                                    val destinationClassNavigability = entity.memberEnds.last().navigability
                                    val createAssociationModel = CreateAssociationModel(
                                        sourceClass.name,
                                        sourceClassNavigability,
                                        destinationClass.name,
                                        destinationClassNavigability,
                                        entity.name,
                                    )
                                    createTransaction.operations.add(createAssociationModel)
                                    logger.debug("${sourceClass.name}(IClass, $sourceClassNavigability) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass, $destinationClassNavigability)")
                                }
                                else -> {
                                    logger.debug("${sourceClass.name}(IClass) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                                }
                            }
                        }
                        else -> {
                            logger.debug("$sourceClass(Unknown) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                        }
                    }
                }
                is IGeneralization -> {
                    val superClass = entity.superType
                    val subClass = entity.subType
                    val createGeneralizationModel = CreateGeneralizationModel(superClass.name, subClass.name, entity.name)
                    createTransaction.operations.add(createGeneralizationModel)
                    logger.debug("${superClass.name}(IClass) -> ${entity.name}(IGeneralization) - ${subClass.name}(IClass)")
                }
                is IRealization -> {
                    val supplierClass = entity.supplier
                    val clientClass = entity.client
                    val createRealizationModel = CreateRealizationModel(supplierClass.name, clientClass.name, entity.name)
                    createTransaction.operations.add(createRealizationModel)
                    logger.debug("${supplierClass.name}(IClass) -> ${entity.name}(IRealization) -> ${clientClass.name}(IClass)")
                }
                is IOperation -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            val createOperation = CreateOperation(owner.name, entity.name, entity.returnTypeExpression)
                            createTransaction.operations.add(createOperation)
                            logger.debug("${entity.name}(IOperation) - $owner(IClass)")
                        }
                        else -> {
                            logger.debug("${entity.name}(IOperation) - $owner(Unknown)")
                        }
                    }
                }
                is IAttribute -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            val createAttribute = CreateAttribute(owner.name, entity.name, entity.typeExpression)
                            createTransaction.operations.add(createAttribute)
                            logger.debug("${entity.name}(IAttribute) - ${owner}(IClass)")
                        }
                        else -> {
                            logger.debug("${entity.name}(IAttribute) - ${owner}(Unknown)")
                        }
                    }
                }
//                    is IModel -> {
//                        println("${entity.name}(IModel)")
//                    }
//                    is IPresentation -> {
//                        println("${entity.label}(IPresentation)")
//                    }
                is INodePresentation -> {
                    when (val diagram = entity.diagram) {
                        is IClassDiagram -> {
                            when (val model = entity.model) {
                                is IClass -> {
                                    // TODO: クラスとインタフェースをINodePresentationのプロパティで見分ける
                                    val location = Pair(entity.location.x, entity.location.y)
                                    val createClassPresentation = CreateClassPresentation(model.name, location, diagram.name)
                                    createTransaction.operations.add(createClassPresentation)
                                    logger.debug("${entity.label}(INodePresentation)::${model.name}(IClass, ${Pair(entity.width, entity.height)} at ${entity.location})")
                                }
                                else -> {
                                    logger.debug("${entity.label}(INodePresentation) - $model(Unknown)")
                                }
                            }
                        }
                        else -> {
                            logger.debug("${entity.label}(Unknown)")
                        }
                    }
                }
                is ILinkPresentation -> {
                    val source = entity.source.model
                    val target = entity.target.model
                    logger.debug("Model: ${entity.model::class.java}")
                    when (source) {
                        is IClass -> {
                            when (target) {
                                is IClass -> {
                                    when (entity.model) {
                                        is IAssociation -> {
                                            val createLinkPresentation = CreateLinkPresentation(source.name, target.name, "Association", entity.diagram.name)
                                            createTransaction.operations.add(createLinkPresentation)
                                            logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IAssociation) - ${target.name}(IClass)")
                                        }
                                        is IGeneralization -> {
                                            val createLinkPresentation = CreateLinkPresentation(source.name, target.name, "Generalization", entity.diagram.name)
                                            createTransaction.operations.add(createLinkPresentation)
                                            logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IGeneralization) - ${target.name}(IClass)")
                                        }
                                        is IRealization -> {
                                            val createLinkPresentation = CreateLinkPresentation(source.name, target.name, "Realization", entity.diagram.name)
                                            createTransaction.operations.add(createLinkPresentation)
                                            logger.debug("${source.name}(IClass, interface) - ${entity.label}(ILinkPresentation::IRealization) - ${target.name}(IClass)")
                                        }
                                        else -> {
                                            logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::Unknown) - ${target.name}(IClass)")
                                        }
                                    }
                                }
                                else -> {
                                    logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                                }
                            }
                        }
                        else -> {
                            logger.debug("$source(Unknown) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                        }
                    }
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                }
            }
        }
        if (createTransaction.operations.isNotEmpty()) {
            ProjectChangedListener.encodeAndPublish(createTransaction, mqttPublisher)
            return
        }

        val modifyProjectEditUnit = projectEditUnit.filter { it.operation == Operation.MODIFY.ordinal }
        for (it in modifyProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is IClass -> {
                    val api = AstahAPI.getAstahAPI()
                    val brotherClassNameList = api.projectAccessor.findElements(IClass::class.java)
                        .filterNot { it.name == entity.name }.map { it?.name }.toList()
                    val changeClassModel = ChangeClassModel(entity.name, brotherClassNameList, entity.stereotypes.toList())
                    modifyTransaction.operations.add(changeClassModel)
                    logger.debug("${entity.name}(IClass) which maybe new name has ${entity.stereotypes.toList()} stereotype")
                }
                is INodePresentation -> {
                    when (val diagram = entity.diagram) {
                        is IClassDiagram -> {
                            when (val model = entity.model) {
                                is IClass -> {
                                    val location = Pair(entity.location.x, entity.location.y)
                                    val size = Pair(entity.width, entity.height)
                                    val resizeClassPresentation = ResizeClassPresentation(model.name, location, size, diagram.name)
                                    modifyTransaction.operations.add(resizeClassPresentation)
                                    logger.debug("${entity.label}(INodePresentation)::${model.name}(IClass, ${Pair(entity.width, entity.height)} at ${entity.location}) @ClassDiagram${diagram.name}")
                                }
                                else -> {
                                    logger.debug("${entity.label}(INodePresentation) - $model(Unknown)")
                                }
                            }
                        }
                        else -> {
                            logger.debug("${entity.label}(INodePresentation) @UnknownDiagram")
                        }
                    }
                }
                is IOperation -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            val brotherNameAndReturnTypeExpression = owner.operations.filterNot { it == entity }
                                .map { Pair(it.name, it.returnTypeExpression) }.toList()
                            val changeOperationNameAndReturnTypeExpression =
                                ChangeOperationNameAndReturnTypeExpression(
                                    owner.name,
                                    brotherNameAndReturnTypeExpression,
                                    entity.name,
                                    entity.returnTypeExpression
                                )
                            modifyTransaction.operations.add(changeOperationNameAndReturnTypeExpression)
                            logger.debug("${entity.name}:${entity.returnTypeExpression}/${entity.returnType}(IOperation) - ${entity.owner}(IClass)")
                            break
                        }
                        else -> {
                            logger.debug("$entity(IOperation) - ${entity.owner}(Unknown)")
                        }
                    }
                }
                is IAttribute -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            val brotherNameAndTypeExpression = owner.attributes.filterNot { it == entity }.map { Pair(it.name, it.typeExpression) }.toList()
                            val changeAttributeNameAndTypeExpression = ChangeAttributeNameAndTypeExpression(owner.name, brotherNameAndTypeExpression, entity.name, entity.typeExpression)
                            modifyTransaction.operations.add(changeAttributeNameAndTypeExpression)
                            logger.debug("${entity.name}:${entity.typeExpression}/${entity.type}(IAttribute) - ${entity.owner}(IClass)")
                            break
                        }
                        else -> {
                            logger.debug("$entity(IAttribute) - ${entity.owner}(Unknown)")
                        }
                    }
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                }
            }
        }
        if (modifyTransaction.operations.isNotEmpty())
            ProjectChangedListener.encodeAndPublish(modifyTransaction, mqttPublisher)
    }

    private fun deleteClassModel(entity: IClass): DeleteClassModel {
        val api = AstahAPI.getAstahAPI()
        val brotherClassNameList = api.projectAccessor.findElements(IClass::class.java)
            .filterNot { it == entity }.map { it?.name }.toList()
        logger.debug("${entity.name}(IClass)")
        return DeleteClassModel(brotherClassNameList)
    }

    private fun deleteAssociationModel(entity: IAssociation, removeProjectEditUnit: List<ProjectEditUnit>): DeleteLinkModel? {
        // TODO: 関連モデルだけで削除された場合に、どの関連モデルが削除されたか認識できるようにする
        return if (removeProjectEditUnit.any { it.entity is ILinkPresentation  && (it.entity as ILinkPresentation).model is IAssociation }) {
            logger.debug("${entity.name}(IAssociation")
            DeleteLinkModel(true)
        } else {
            logger.debug("$entity(IAssociation)")
            logger.warn("This operation does not support because plugin can't detect what association model delete.")
            null
        }
    }

    private fun deleteGeneralizationModel(entity: IGeneralization, removeProjectEditUnit: List<ProjectEditUnit>): DeleteLinkModel? {
        // TODO: 汎化モデルだけで削除された場合に、どの汎化モデルが削除されたか認識できるようにする
        return if (removeProjectEditUnit.any { it.entity is ILinkPresentation  && (it.entity as ILinkPresentation).model is IGeneralization }) {
            logger.debug("${entity.name}(IGeneralization")
            DeleteLinkModel(true)
        } else {
            logger.debug("$entity(IGeneralization)")
            logger.warn("This operation does not support because plugin can't detect what generalization model delete.")
            null
        }
    }

    private fun deleteRealizationModel(entity: IRealization, removeProjectEditUnit: List<ProjectEditUnit>): DeleteLinkModel? {
        // TODO: 実現モデルだけで削除された場合に、どの実現モデルが削除されたか認識できるようにする
        return if (removeProjectEditUnit.any { it.entity is ILinkPresentation  && (it.entity as ILinkPresentation).model is IRealization }) {
            logger.debug("${entity.name}(IRealization")
            DeleteLinkModel(true)
        } else {
            logger.debug("$entity(IRealization)")
            logger.warn("This operation does not support because plugin can't detect what realization model delete.")
            null
        }
    }

    private fun deleteLinkPresentation(entity: ILinkPresentation, removeProjectEditUnit: List<ProjectEditUnit>): DeleteLinkPresentation? {
        if (removeProjectEditUnit.any { it.entity is IClass }) {
            return null
        }
        return when (val model = entity.model) {
            is IAssociation -> {
                val serializablePoints = entity.points.map { point->Pair(point.x,point.y) }.toList()
                logger.debug("${entity.label}(ILinkPresentation, IAssociation)")
                DeleteLinkPresentation(serializablePoints, "Association")
            }
            is IGeneralization -> {
                val serializablePoints = entity.points.map { point->Pair(point.x,point.y) }.toList()
                logger.debug("${model.name}(ILinkPresentation, IGeneralization)")
                DeleteLinkPresentation(serializablePoints, "Generalization")
            }
            is IRealization -> {
                val serializablePoints = entity.points.map { point->Pair(point.x,point.y) }.toList()
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

    companion object: Logging {
        private val logger = logger()
    }
}