/*
 * MindmapDiagramApplyTransaction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.model.IMindMapDiagram
import com.change_vision.jude.api.inf.model.INamedElement
import com.change_vision.jude.api.inf.presentation.INodePresentation
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import java.awt.geom.Point2D

class MindmapDiagramApplyTransaction(private val entityTable: EntityTable) :
    IApplyTransaction<MindmapDiagramOperation> {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    private val mindmapEditor = projectAccessor.diagramEditorFactory.mindmapEditor

    @Throws(BadTransactionException::class)
    override fun apply(operations: List<MindmapDiagramOperation>) {
        operations.forEach { it ->
            when (it) {
                is CreateMindmapDiagram -> {
                    validateAndCreateMindmapDiagram(it)
                }
                is CreateTopic -> {
                    validateAndCreateTopic(it)
                }
                is CreateFloatingTopic -> {
                    validateAndCreateFloatingTopic(it)
                }
                is ModifyTopic -> {
                    validateAndModifyTopic(it)
                }
                is DeleteMindmapDiagram -> {
                    validateAndDeleteMindMap(it)
                }
                is DeleteTopic -> {
                    validateAndDeleteTopic(it)
                }
            }
        }
    }

    private fun validateAndCreateMindmapDiagram(operation: CreateMindmapDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty() && operation.rootTopicId.isNotEmpty() && operation.id.isNotEmpty()) {
            createMindmapDiagram(operation.name, operation.ownerName, operation.rootTopicId, operation.id)
        }
    }

    private fun validateAndCreateTopic(operation: CreateTopic) {
        if (operation.ownerId.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.diagramId.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createTopic(operation.ownerId, operation.name, operation.diagramId, operation.id)
        }
    }

    private fun validateAndCreateFloatingTopic(operation: CreateFloatingTopic) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.name.isNotEmpty() && operation.diagramId.isNotEmpty() && operation.id.isNotEmpty()) {
            createFloatingTopic(operation.name, location, operation.size, operation.diagramId, operation.id)
        }
    }

    private fun validateAndModifyTopic(operation: ModifyTopic) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.name.isNotEmpty() && operation.diagramId.isNotEmpty() && operation.id.isNotEmpty()) {
            modifyTopic(operation.name, location, operation.size, operation.parentId, operation.diagramId, operation.id)
        }
    }

    private fun validateAndDeleteMindMap(operation: DeleteMindmapDiagram) {
        if (operation.id.isNotEmpty()) {
            deleteMindmapDiagram(operation.id)
        }
    }

    private fun validateAndDeleteTopic(operation: DeleteTopic) {
        if (operation.id.isNotEmpty() && operation.diagramId.isNotEmpty()) {
            deleteTopic(operation.id, operation.diagramId)
        }
    }

    private fun createMindmapDiagram(name: String, ownerName: String, rootTopicId: String, id: String) {
        logger.debug("Create mindmap diagram.")
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = mindmapEditor.createMindmapDiagram(owner, name)
        val rootTopic = diagram.root
        entityTable.entries.add(Entry(diagram.id, id))
        entityTable.entries.add(Entry(rootTopic.id, rootTopicId))
        diagramViewManager.open(diagram)
    }

    private fun searchTopic(targetId: String, topics: Array<INodePresentation>): INodePresentation? {
        topics.forEach {
            if (it.id == targetId) {
                return it
            } else if (it.children.isNotEmpty()) {
                val topic = searchTopic(targetId, it.children)
                if (topic != null) {
                    return topic
                }
            }
        }
        return null
    }

    private fun createTopic(ownerId: String, name: String, diagramId: String, id: String) {
        logger.debug("Create topic.")
        val diagramEntry = entityTable.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java).find { it.id == diagramEntry.mine } as IMindMapDiagram?
                ?: run {
                    logger.debug("IMindMapDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        mindmapEditor.diagram = diagram
        val topics = diagram.floatingTopics + diagram.root
        val parentEntry = entityTable.entries.find { it.common == ownerId }
        if (parentEntry == null) {
            logger.debug("$ownerId not found on LUT.")
            return
        }
        val parent = searchTopic(parentEntry.mine, topics)
        if (parent == null) {
            logger.error("Parent topic not found.")
            return
        }
        val topic = mindmapEditor.createTopic(parent, name)
        entityTable.entries.add(Entry(topic.id, id))
    }

    private fun createFloatingTopic(
        name: String,
        location: Point2D,
        size: Pair<Double, Double>,
        diagramId: String,
        id: String
    ) {
        logger.debug("Create floating topic.")
        val (width, height) = size
        val diagramEntry = entityTable.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java).find { it.id == diagramEntry.mine } as IMindMapDiagram?
                ?: run {
                    logger.debug("IMindMapDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        mindmapEditor.diagram = diagram
        val parentTopic = diagram.root
        val topic = mindmapEditor.createTopic(parentTopic, name)
        mindmapEditor.changeToFloatingTopic(topic)
        topic.location = location
        topic.width = width
        topic.height = height
        entityTable.entries.add(Entry(topic.id, id))
    }

    private fun modifyTopic(
        name: String,
        location: Point2D,
        size: Pair<Double, Double>,
        parentId: String,
        diagramId: String,
        id: String
    ) {
        logger.debug("Modify topic.")
        val (width, height) = size
        val diagramEntry = entityTable.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java).find { it.id == diagramEntry.mine } as IMindMapDiagram?
                ?: run {
                    logger.debug("IMindMapDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        mindmapEditor.diagram = diagram
        val topics = diagram.floatingTopics + diagram.root
        val entry = entityTable.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val topic = searchTopic(entry.mine, topics) ?: run {
            logger.debug("Topic ${entry.mine} not found but $id found on LUT.")
            return
        }

        if (topic == diagram.root) {
            topic.label = name
            return
        }

        if (parentId.isEmpty()) {
            if (topic.parent != null) {
                logger.debug("Change to floating topic.")
                mindmapEditor.changeToFloatingTopic(topic)
            } else {
                topic.label = name
                topic.location = location
                topic.width = width
                topic.height = height
            }
        } else {
            val parentEntry = entityTable.entries.find { it.common == parentId } ?: run {
                logger.debug("$parentId not found on LUT.")
                return
            }
            val parentTopic = searchTopic(parentEntry.mine, topics) ?: run {
                logger.debug("Topic ${parentEntry.mine} not found but $parentId found on LUT.")
                return
            }

            if (topic.parent != parentTopic) {
                logger.debug("Move topic parent: $parentTopic -> child: $topic.")
                mindmapEditor.moveTo(topic, parentTopic)
            }
            topic.label = name
        }
    }

    private fun deleteMindmapDiagram(id: String) {
        logger.debug("Delete mindmap diagram.")
        val lutEntry = entityTable.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val diagram = projectAccessor.project.diagrams.find { it.id == lutEntry.mine } ?: run {
            logger.debug("IDiagram ${lutEntry.mine} not found but $id found on LUT.")
            entityTable.entries.remove(lutEntry)
            return
        }
        val rootTopic = (diagram as IMindMapDiagram).root
        val rootEntry = entityTable.entries.find { it.mine == rootTopic.id } ?: run {
            logger.debug("Root topic ${rootTopic.id} not found on LUT.")
            entityTable.entries.remove(lutEntry)
            mindmapEditor.diagram = diagram
            mindmapEditor.deleteDiagram()
        }
        // TODO: 削除されるとプレゼンテーションのエントリが残り続ける
        entityTable.entries.remove(rootEntry)
        entityTable.entries.remove(lutEntry)
        mindmapEditor.diagram = diagram
        mindmapEditor.deleteDiagram()
    }

    private fun deleteTopic(id: String, diagramId: String) {
        logger.debug("Delete topic.")
        val diagramEntry = entityTable.entries.find { it.common == diagramId } ?: run {
            logger.debug("$diagramId not found on LUT.")
            return
        }
        val diagram =
            projectAccessor.findElements(IDiagram::class.java).find { it.id == diagramEntry.mine } as IMindMapDiagram?
                ?: run {
                    logger.debug("IMindMapDiagram ${diagramEntry.mine} not found but $diagramId found on LUT.")
                    return
                }
        mindmapEditor.diagram = diagram
        val topics = diagram.floatingTopics + diagram.root
        val topicEntry = entityTable.entries.find { it.common == id }
        if (topicEntry == null) {
            logger.debug("$id not found on LUT.")
            return
        }
        val topic = searchTopic(topicEntry.mine, topics)
        if (topic == null) {
            logger.debug("Topic ${topicEntry.mine} not found but $id found on LUT.")
            entityTable.entries.remove(topicEntry)
        } else {
            mindmapEditor.deletePresentation(topic)
            entityTable.entries.remove(topicEntry)
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}