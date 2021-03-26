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
        println("===== Transaction detected =====")
        val projectEditUnit = e.projectEditUnit.filter { it.entity != null }
//        val removeTransaction = Transaction()
        val createTransaction = Transaction()
        val modifyTransaction = Transaction()
//        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
//        for (it in removeProjectEditUnit){
//            val operation = Operation.values()[it.operation]
//            print("Op: $operation -> ")
//            when (val entity = it.entity) {
//                is IClass -> {
//                    val deleteClassModel = DeleteClassModel(entity.name)
//                    removeTransaction.deleteClassModel = deleteClassModel
//                    println("${entity.name}(IClass)")
//                }
//                is ILinkPresentation -> {
//                    when (val model = entity.model) {
//                        is IAssociation -> {
//                            val memberEnds = model.memberEnds
//                            val source = memberEnds.first().owner
//                            val target = memberEnds.last().owner
//                            when (source) {
//                                is IClass -> {
//                                    when (target) {
//                                        is IClass -> {
//                                            val deleteAssociationPresentation = DeleteAssociationPresentation(source.name, target.name, entity.diagram.name)
//                                            createTransaction.deleteAssociationPresentation = deleteAssociationPresentation
//                                            println("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - ${target.name}(IClass)")
//                                        }
//                                        else -> {
//                                            println("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
//                                        }
//                                    }
//                                }
//                                else -> {
//                                    println("$source(Unknown) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
//                                }
//                            }
//                        }
//                        else -> {
//                            println("$entity(INodePresentation)")
//                        }
//                    }
//                }
//                else -> {
//                    println("$entity(Unknown)")
//                }
//            }
//        }
//        if (removeTransaction.isNotAllNull())
//            encodeAndPublish(removeTransaction)
        val addProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        for (it in addProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            print("Op: $operation -> ")
            when (val entity = it.entity) {
                is IClassDiagram -> {
                    val owner = entity.owner as INamedElement
                    val createClassDiagram = CreateClassDiagram(entity.name, owner.name)
                    createTransaction.createClassDiagram = createClassDiagram
                    println("${entity.name}(IClassDiagram)")
                    break
                }
                is IMindMapDiagram -> {
                    val owner = entity.owner as INamedElement
                    val createMindMapDiagram = CreateMindMapDiagram(entity.name, owner.name)
                    createTransaction.createMindMapDiagram = createMindMapDiagram
                    println("${entity.name}(IMindMapDiagram)")
                    break
                }
                is IClass -> {
                    val parentPackage = entity.owner as IPackage
                    val createClassModel = CreateClassModel(entity.name, parentPackage.name)
                    createTransaction.createClassModel = createClassModel
                    println("${entity.name}(IClass)")
                }
                is IAssociation -> {
                    val sourceClass = entity.memberEnds.first().owner
                    val destinationClass = entity.memberEnds.last().owner
                    when (sourceClass) {
                        is IClass -> {
                            when (destinationClass) {
                                is IClass -> {
                                    val createAssociationModel = CreateAssociationModel(sourceClass.name, destinationClass.name, entity.name)
                                    createTransaction.createAssociationModel = createAssociationModel
                                    println("${sourceClass.name}(IClass) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass)")
                                }
                                else -> {
                                    println("${sourceClass.name}(IClass) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                                }
                            }
                        }
                        else -> {
                            println("$sourceClass(Unknown) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                        }
                    }
                }
                is IOperation -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            createTransaction.createOperation = CreateOperation(owner.name, entity.name, entity.returnTypeExpression)
                            println("${entity.name}(IOperation) - $owner(IClass)")
                        }
                        else -> {
                            println("${entity.name}(IOperation) - $owner(Unknown)")
                        }
                    }
                }
                is IAttribute -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            createTransaction.createAttribute = CreateAttribute(owner.name, entity.name, entity.typeExpression)
                            println("${entity.name}(IAttribute) - ${owner}(IClass)")
                        }
                        else -> {
                            println("${entity.name}(IAttribute) - ${owner}(Unknown)")
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
                                    createTransaction.createClassPresentation = createClassPresentation
                                    println("${entity.label}(INodePresentation)::${model.name}(IClass, ${Pair(entity.width, entity.height)} at ${entity.location})")
                                }
                                else -> {
                                    println("${entity.label}(INodePresentation) - $model(Unknown)")
                                }
                            }
                        }
                        is IMindMapDiagram -> {
                            when (val owner = entity.parent) {
                                null -> {
                                    val location = Pair(entity.location.x, entity.location.y)
                                    val size = Pair(entity.width, entity.height)
                                    val createFloatingTopic = CreateFloatingTopic(entity.label, location, size, diagram.name)
                                    createTransaction.createFloatingTopic = createFloatingTopic
                                    println("${entity.label}(INodePresentation, FloatingTopic)")
                                }
                                else -> {
                                    val createTopic = CreateTopic(owner.label, entity.label, diagram.name)
                                    createTransaction.createTopic = createTopic
                                    println("${owner.label}(INodePresentation) - ${entity.label}(INodePresentation)")
                                }
                            }
                        }
                        else -> {
                            println("${entity.label}(Unknown)")
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
                                    createTransaction.createAssociationPresentation = createAssociationPresentation
                                    println("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - ${target.name}(IClass)")
                                }
                                else -> {
                                    println("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                                }
                            }
                        }
                        else -> {
                            println("$source(Unknown) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                        }
                    }
                }
                else -> {
                    println("$entity(Unknown)")
                }
            }
        }
        if (createTransaction.isNotAllNull()) {
            encodeAndPublish(createTransaction)
            return
        }
        val modifyProjectEditUnit = projectEditUnit.filter { it.operation == Operation.MODIFY.ordinal }
        for (it in modifyProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            print("Op: $operation -> ")
            when (val entity = it.entity) {
                is INodePresentation -> {
                    when (val diagram = entity.diagram) {
                        is IClassDiagram -> {
                            when (val model = entity.model) {
                                is IClass -> {
                                    val location = Pair(entity.location.x, entity.location.y)
                                    val size = Pair(entity.width, entity.height)
                                    val resizeClassPresentation = ResizeClassPresentation(model.name, location, size, diagram.name)
                                    modifyTransaction.resizeClassPresentation = resizeClassPresentation
                                    println("${entity.label}(INodePresentation)::${model.name}(IClass, ${Pair(entity.width, entity.height)} at ${entity.location}) @ClassDiagram${diagram.name}")
                                }
                                else -> {
                                    println("${entity.label}(INodePresentation) - $model(Unknown)")
                                }
                            }
                        }
                        is IMindMapDiagram -> {
                            val location = Pair(entity.location.x, entity.location.y)
                            val size = Pair(entity.width, entity.height)
                            val resizeTopic = ResizeTopic(entity.label, location, size, diagram.name)
                            modifyTransaction.resizeTopic = resizeTopic
                            println("${entity.label}(INodePresentation)::Topic(${Pair(entity.width, entity.height)} at ${entity.location}) @MindmapDiagram${diagram.name}")
                            break
                        }
                        else -> {
                            println("${entity.label}(INodePresentation) @UnknownDiagram")
                        }
                    }
                }
                is IAttribute -> {
                    when (val owner = entity.owner) {
                        is IClass -> {
                            val brotherNameAndTypeExpression = owner.attributes.filterNot { it == entity }.map { Pair(it.name, it.typeExpression) }.toList()
                            modifyTransaction.changeAttributeNameAndTypeExpression = ChangeAttributeNameAndTypeExpression(owner.name, brotherNameAndTypeExpression, entity.name, entity.typeExpression)
                            println("${entity.name}:${entity.typeExpression}/${entity.type}(IAttribute) - ${entity.owner}(IClass)")
                            break
                        }
                        else -> {
                            println("$entity(IAttribute) - ${entity.owner}(Unknown)")
                        }
                    }
                }
                else -> {
                    println("$entity(Unknown)")
                }
            }
        }
        if (modifyTransaction.isNotAllNull())
            encodeAndPublish(modifyTransaction)
    }

    @ExperimentalSerializationApi
    private fun encodeAndPublish(transaction: Transaction) {
        val byteArray = Cbor.encodeToByteArray(transaction)
        mqttPublisher.publish(byteArray)
    }

    override fun projectOpened(p0: ProjectEvent) {}
    override fun projectClosed(p0: ProjectEvent) {}
}