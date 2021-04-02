/*
 * MindmapDiagramEventListener.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.model.IMindMapDiagram
import com.change_vision.jude.api.inf.model.INamedElement
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectEditUnit
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi

class MindmapDiagramEventListener(private val mqttPublisher: MqttPublisher): IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
//        val removeTransaction = Transaction()
        val createTransaction = Transaction()
        val modifyTransaction = Transaction()
//        val removeProjectEditUnit = projectEditUnit.filter { it.operation == Operation.REMOVE.ordinal }
//        for (it in removeProjectEditUnit) {
//            val operation = Operation.values()[it.operation]
//            logger.debug("Op: $operation -> ")
//            when (val entity = it.entity) {
//                is ILinkPresentation -> {
//                    when (entity.model) {
//                        else -> {
//                            logger.debug("$entity(INodePresentation)")
//                        }
//                    }
//                }
//                else -> {
//                    logger.debug("$entity(Unknown)")
//                }
//            }
//        }
//        if (removeTransaction.operations.isNotEmpty()) {
//            ProjectChangedListener.encodeAndPublish(removeTransaction, mqttPublisher)
//            return
//        }

        val addProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        for (it in addProjectEditUnit) {
            val operation = Operation.values()[it.operation]
            logger.debug("Op: $operation -> ")
            when (val entity = it.entity) {
                is IMindMapDiagram -> {
                    modifyTransaction.operations
                        .add(createMindMapDiagram(entity))
                    break
                }
                is INodePresentation -> {
                    when (entity.diagram) {
                        is IMindMapDiagram -> {
                            when (entity.parent) {
                                null -> {
                                    modifyTransaction.operations
                                        .add(createFloatingTopic(entity))
                                }
                                else -> {
                                    modifyTransaction.operations
                                        .add(createTopic(entity))
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
                    logger.debug("$source(Unknown) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
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
                    modifyTransaction.operations
                        .add(
                            resizeTopic(entity)
                                ?: continue
                        )
                    break
                }
                else -> {
                    logger.debug("$entity(Unknown)")
                }
            }
        }
        if (modifyTransaction.operations.isNotEmpty()) {
            ProjectChangedListener.encodeAndPublish(modifyTransaction, mqttPublisher)
        }
    }

    private fun createMindMapDiagram(entity: IMindMapDiagram): CreateMindmapDiagram {
        val owner = entity.owner as INamedElement
        logger.debug("${entity.name}(IMindMapDiagram)")
        return CreateMindmapDiagram(entity.name, owner.name)
    }

    private fun createFloatingTopic(entity: INodePresentation): CreateFloatingTopic {
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        logger.debug("${entity.label}(INodePresentation, FloatingTopic)")
        return CreateFloatingTopic(entity.label, location, size, entity.diagram.name)
    }

    private fun createTopic(entity: INodePresentation): CreateTopic {
        logger.debug("${entity.parent.label}(INodePresentation) - ${entity.label}(INodePresentation)")
        return CreateTopic(entity.parent.label, entity.label, entity.diagram.name)
    }

    private fun resizeTopic(entity: INodePresentation): ResizeTopic? {
        return when (val diagram = entity.diagram) {
            is IMindMapDiagram -> {
                val location = Pair(entity.location.x, entity.location.y)
                val size = Pair(entity.width, entity.height)
                logger.debug("${entity.label}(INodePresentation)::Topic(${Pair(entity.width, entity.height)} at ${entity.location}) @MindmapDiagram${diagram.name}")
                ResizeTopic(entity.label, location, size, diagram.name)
            }
            else -> {
                logger.debug("${entity.label}(INodePresentation) @UnknownDiagram")
                null
            }
        }
    }

    companion object: Logging {
        private val logger = logger()
    }
}