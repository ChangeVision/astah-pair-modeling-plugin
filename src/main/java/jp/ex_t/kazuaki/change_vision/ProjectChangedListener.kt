/*
 * ProjectChangedListener.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.project.ProjectEventListener
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray

class ProjectChangedListener(private val mqttPublisher: MqttPublisher): ProjectEventListener {
    @ExperimentalSerializationApi
    override fun projectChanged(e: ProjectEvent) {
        logger.debug("===== Transaction detected =====")
        val projectEditUnit = e.projectEditUnit.filter { it.entity != null }
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
                            logger.debug("$entity(INodePresentation)")
                        }
                    }
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                }
            }
        }
        if (removeTransaction.operations.isNotEmpty()){
            encodeAndPublish(removeTransaction)
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
                is IMindMapDiagram -> {
                    val owner = entity.owner as INamedElement
                    val createMindMapDiagram = CreateMindmapDiagram(entity.name, owner.name)
                    createTransaction.operations.add(createMindMapDiagram)
                    logger.debug("${entity.name}(IMindMapDiagram)")
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
                        is IMindMapDiagram -> {
                            when (val owner = entity.parent) {
                                null -> {
                                    val location = Pair(entity.location.x, entity.location.y)
                                    val size = Pair(entity.width, entity.height)
                                    val createFloatingTopic = CreateFloatingTopic(entity.label, location, size, diagram.name)
                                    createTransaction.operations.add(createFloatingTopic)
                                    logger.debug("${entity.label}(INodePresentation, FloatingTopic)")
                                }
                                else -> {
                                    val createTopic = CreateTopic(owner.label, entity.label, diagram.name)
                                    createTransaction.operations.add(createTopic)
                                    logger.debug("${owner.label}(INodePresentation) - ${entity.label}(INodePresentation)")
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
            encodeAndPublish(createTransaction)
            return
        }
        val modifyProjectEditUnit = projectEditUnit.filter { it.operation == Operation.MODIFY.ordinal }
        for (it in modifyProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
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
                        is IMindMapDiagram -> {
                            val location = Pair(entity.location.x, entity.location.y)
                            val size = Pair(entity.width, entity.height)
                            val resizeTopic = ResizeTopic(entity.label, location, size, diagram.name)
                            modifyTransaction.operations.add(resizeTopic)
                            logger.debug("${entity.label}(INodePresentation)::Topic(${Pair(entity.width, entity.height)} at ${entity.location}) @MindmapDiagram${diagram.name}")
                            break
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
            encodeAndPublish(modifyTransaction)
    }

    @ExperimentalSerializationApi
    private fun encodeAndPublish(transaction: Transaction) {
        val byteArray = Cbor.encodeToByteArray(transaction)
        mqttPublisher.publish(byteArray)
    }

    override fun projectOpened(p0: ProjectEvent) {}
    override fun projectClosed(p0: ProjectEvent) {}

    companion object: Logging {
        private val logger = logger()
    }
}