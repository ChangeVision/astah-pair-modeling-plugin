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
        for (it in removeProjectEditUnit){
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is IClass -> {
                    val deleteClassModel = DeleteClassModel(entity.name)
                    removeTransaction.operations.add(deleteClassModel)
                    logger.debug("${entity.name}(IClass)")
                }
                is IAssociation -> {
                    if (entity.name.isNullOrBlank()) {
                        // TODO: 削除された関連モデルがどれなのか見分けられるようにする
                        removeTransaction.operations.add(DeleteAssociationModel(true))
                        logger.debug("${entity.name}(IAssociation")
                    }
                }
                is ILinkPresentation -> {
                    if (removeProjectEditUnit.any { it.entity is IClass })
                        continue
                    when (val model = entity.model) {
                        is IAssociation -> {
                            val memberEnds = model.memberEnds
                            val source = memberEnds.first().owner
                            val target = memberEnds.last().owner
                            when (source) {
                                is IClass -> {
                                    when (target) {
                                        is IClass -> {
                                            val serializablePoints = entity.points.map { point->Pair(point.x,point.y) }.toList()
                                            val deleteAssociationPresentation = DeleteAssociationPresentation(serializablePoints)
                                            removeTransaction.operations.add(deleteAssociationPresentation)
                                            logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - ${target.name}(IClass)")
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
                            logger.debug("$entity(ILinkPresentation)")
                        }
                    }
                }
                is INodePresentation -> {
                    when (val model = entity.model) {
                        is IClass -> {
                            val deleteClassPresentation = DeleteClassPresentation(model.name)
                            removeTransaction.operations.add(deleteClassPresentation)
                            logger.debug("${model.name}(INodePresentation, IClass)")
                        }
                    }
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
                    val createClassModel = CreateClassModel(entity.name, parentPackage.name)
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
                                    val createAssociationModel = CreateAssociationModel(sourceClass.name, destinationClass.name, entity.name)
                                    createTransaction.operations.add(createAssociationModel)
                                    logger.debug("${sourceClass.name}(IClass) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass)")
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
                    when (source) {
                        is IClass -> {
                            when (target) {
                                is IClass -> {
                                    val createAssociationPresentation = CreateAssociationPresentation(source.name, target.name, entity.diagram.name)
                                    createTransaction.operations.add(createAssociationPresentation)
                                    logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - ${target.name}(IClass)")
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
                    val changeClassModelName = ChangeClassModelName(entity.name, brotherClassNameList)
                    modifyTransaction.operations.add(changeClassModelName)
                    logger.debug("${entity.name}(IClass) maybe new name")
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

    companion object: Logging {
        private val logger = logger()
    }
}