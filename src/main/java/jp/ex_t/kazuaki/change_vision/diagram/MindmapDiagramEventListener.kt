/*
 * MindmapDiagramEventListener.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.diagram

import com.change_vision.jude.api.inf.model.IMindMapDiagram
import com.change_vision.jude.api.inf.model.INamedElement
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectEditUnit
import jp.ex_t.kazuaki.change_vision.*
import kotlinx.serialization.ExperimentalSerializationApi

class MindmapDiagramEventListener(private val mqttPublisher: MqttPublisher): IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        val removeTransaction = Transaction()
        val createTransaction = Transaction()
        val modifyTransaction = Transaction()
        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
        for (it in removeProjectEditUnit){
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is ILinkPresentation -> {
                    when (val model = entity.model) {
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
        if (removeTransaction.operations.isNotEmpty()) {
            ProjectChangedListener.encodeAndPublish(removeTransaction, mqttPublisher)
            return
        }

        val addProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        for (it in addProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is IMindMapDiagram -> {
                    val owner = entity.owner as INamedElement
                    val createMindMapDiagram = CreateMindmapDiagram(entity.name, owner.name)
                    createTransaction.operations.add(createMindMapDiagram)
                    logger.debug("${entity.name}(IMindMapDiagram)")
                    break
                }
                is INodePresentation -> {
                    when (val diagram = entity.diagram) {
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
                is INodePresentation -> {
                    when (val diagram = entity.diagram) {
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