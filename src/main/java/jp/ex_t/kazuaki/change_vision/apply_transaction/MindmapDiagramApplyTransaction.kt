/*
 * MindmapDiagramApplyTransaction.kt - pair-modeling-prototype
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

class MindmapDiagramApplyTransaction: IApplyTransaction<MindmapDiagramOperation> {
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
                is ResizeTopic -> {
                    validateAndResizeTopic(it)
                }
            }
        }
    }

    private fun validateAndCreateMindmapDiagram(operation: CreateMindmapDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty()) {
            createMindmapDiagram(operation.name, operation.ownerName)
        }
    }

    private fun validateAndCreateTopic(operation: CreateTopic) {
        if (operation.ownerName.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.diagramName.isNotEmpty()) {
                createTopic(operation.ownerName, operation.name, operation.diagramName)
        }
    }

    private fun validateAndCreateFloatingTopic(operation: CreateFloatingTopic) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.name.isNotEmpty() && operation.diagramName.isNotEmpty()) {
            createFloatingTopic(operation.name, location, operation.size, operation.diagramName)
        }
    }

    private fun validateAndResizeTopic(operation: ResizeTopic) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.name.isNotEmpty() && operation.diagramName.isNotEmpty()) {
            resizeTopic(operation.name, location, operation.size, operation.diagramName)
        }
    }

    private fun createMindmapDiagram(name: String, ownerName: String) {
        logger.debug("Create mindmap diagram.")
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = mindmapEditor.createMindmapDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    private fun searchTopic(name: String, topics: Array<INodePresentation>): INodePresentation? {
        topics.forEach {
            if (it.label == name) {
                return it
            }
            else if (it.children.isNotEmpty()) {
                return searchTopic(name, it.children)
            }
        }
        return null
    }

    private fun createTopic(ownerName: String, name: String, diagramName: String) {
        logger.debug("Create topic.")
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IMindMapDiagram
        mindmapEditor.diagram = diagram
        val topics = diagram.floatingTopics + diagram.root
        val parent = searchTopic(ownerName, topics)
        if (parent == null) {
            logger.error("Parent topic not found.")
            return
        }
        mindmapEditor.createTopic(parent, name)
    }

    private fun createFloatingTopic(name: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        logger.debug("Create floating topic.")
        val (width, height) = size
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IMindMapDiagram
        mindmapEditor.diagram = diagram
        val parentTopic = diagram.root
        val topic = mindmapEditor.createTopic(parentTopic, name)
        mindmapEditor.changeToFloatingTopic(topic)
        topic.location = location
        topic.width = width
        topic.height = height
    }

    private fun resizeTopic(name: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        logger.debug("Resize topic.")
        val (width, height) = size
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IMindMapDiagram
        mindmapEditor.diagram = diagram
        val topic = diagram.floatingTopics.first { it.label == name }
        topic.location = location
        topic.width = width
        topic.height = height
    }
    
    companion object: Logging {
        private val logger = logger()
    }
}