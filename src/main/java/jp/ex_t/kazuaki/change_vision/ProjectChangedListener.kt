/*
 * ProjectChangedListener.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.model.IAssociation
import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.model.IModel
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
        projectEditUnit
            .forEach {
                val operation = Operation.values()[it.operation]
                when (val entity = it.entity) {
                    is IClass -> {
                        if (operation == Operation.ADD) {
                            val model = entity.owner as IModel
                            val createClassModel = CreateClassModel(entity.name, model.name)
                            val transaction = Transaction(createCreateClassModel=createClassModel)
                            val byteArray = Cbor.encodeToByteArray(transaction)
                            mqttPublisher.publish(byteArray)
                        }
                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IClass)")
                    }
                    is IAssociation -> {
                        val sourceClass = entity.memberEnds.first().owner
                        val destinationClass = entity.memberEnds.last().owner
                        when (sourceClass) {
                            is IClass -> when (destinationClass) {
                                is IClass -> {
                                    if (operation == Operation.ADD) {
                                        val createAssociationModel = CreateAssociationModel(sourceClass.name, destinationClass.name, entity.name)
                                        val transaction = Transaction(createCreateAssociationModel = createAssociationModel)
                                        val byteArray = Cbor.encodeToByteArray(transaction)
                                        mqttPublisher.publish(byteArray)
                                    }
                                    println("Op: ${Operation.values()[it.operation]} -> ${sourceClass.name} - ${entity.name}(IAssociation) - ${destinationClass.name}")
                                }
                            }
                        }
                    }
//                    is IAttribute -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IAttribute)")
//                    }
//                    is IOperation -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IOperation)")
//                    }
//                    is IModel -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IModel)")
//                    }
//                    is IPresentation -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.label}(IPresentation)")
//                    }
                    is INodePresentation -> {
                            when (val model = entity.model) {
                                is IClass -> {
                                    if (operation == Operation.ADD) {
                                        val location = Pair(entity.location.x, entity.location.y)
                                        val createClassPresentation =
                                            CreateClassPresentation(model.name, location, entity.diagram.name)
                                        val transaction = Transaction(createClassPresentation = createClassPresentation)
                                        val byteArray = Cbor.encodeToByteArray(transaction)
                                        mqttPublisher.publish(byteArray)
                                    }
                                    println("Op: ${Operation.values()[it.operation]} -> ${entity.label}(INodePresentation)::${model.name}(IClass)")
                                }
                                else -> {
                                    println("Op: ${Operation.values()[it.operation]} -> ${entity.label}(INodePresentation) - $model(Unknown)")
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
                                        if (operation == Operation.ADD) {
                                            val createAssociationPresentation = CreateAssociationPresentation(source.name, target.name, entity.diagram.name)
                                            val transaction = Transaction(createAssociationPresentation = createAssociationPresentation)
                                            val byteArray = Cbor.encodeToByteArray(transaction)
                                            mqttPublisher.publish(byteArray)
                                        }
                                        println("Op: ${Operation.values()[it.operation]} -> ${source.name}(IClass) - ${entity.label}(ILinkPresentation) - ${target.name}(IClass)")
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        println("Op: ${Operation.values()[it.operation]} -> ${entity}(Unknown)")
                    }
                }
            }
    }

    override fun projectOpened(p0: ProjectEvent) {}
    override fun projectClosed(p0: ProjectEvent) {}
}