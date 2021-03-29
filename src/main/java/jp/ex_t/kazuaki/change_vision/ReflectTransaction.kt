/*
 * ReflectTransaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import java.awt.geom.Point2D
import javax.swing.SwingUtilities

class ReflectTransaction(private val projectChangedListener: ProjectChangedListener) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor

    @Throws(UnExpectedException::class)
    fun transact(transaction: Transaction) {
        SwingUtilities.invokeLater {
            val transactionManager = projectAccessor.transactionManager
            try {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                logger.debug("Start transaction.")
                transactionManager.beginTransaction()
                transaction.operations.forEach { it ->
                    when (it) {
                        is CreateClassDiagram -> {
                            if (it.name.isNotEmpty() && it.ownerName.isNotEmpty())
                                createClassDiagram(it.name, it.ownerName)
                        }
                        is CreateMindmapDiagram -> {
                            if (it.name.isNotEmpty() && it.ownerName.isNotEmpty())
                                createMindMapDiagram(it.name, it.ownerName)
                        }
                        is CreateClassModel -> {
                            if (it.name.isNotEmpty() && it.parentPackageName.isNotEmpty())
                                createClassModel(it.name, it.parentPackageName)
                        }
                        is CreateAssociationModel -> {
                            if (it.sourceClassName.isNotEmpty()
                                && it.destinationClassName.isNotEmpty())
                                    createAssociationModel(
                                        it.sourceClassName,
                                        it.destinationClassName,
                                        it.name
                                    )
                        }
                        is CreateClassPresentation -> {
                            val location = Point2D.Double(it.location.first, it.location.second)
                            if (it.diagramName.isNotEmpty() && it.className.isNotEmpty())
                                createClassPresentation(it.className, location, it.diagramName)
                        }
                        is CreateAssociationPresentation -> {
                            if (it.sourceClassName.isNotEmpty()
                                && it.targetClassName.isNotEmpty()
                                && it.diagramName.isNotEmpty())
                                    createAssociationPresentation(
                                        it.sourceClassName,
                                        it.targetClassName,
                                        it.diagramName
                                    )
                        }
                        is CreateOperation -> {
                            if (it.ownerName.isNotEmpty()
                                && it.name.isNotEmpty()
                                && it.returnTypeExpression.isNotEmpty())
                                    createOperation(it.ownerName, it.name, it.returnTypeExpression)
                        }
                        is CreateAttribute -> {
                            if (it.ownerName.isNotEmpty()
                                && it.name.isNotEmpty()
                                && it.typeExpression.isNotEmpty())
                                    createAttribute(it.ownerName, it.name, it.typeExpression)
                        }
                        is CreateTopic -> {
                            if (it.ownerName.isNotEmpty()
                                && it.name.isNotEmpty()
                                && it.diagramName.isNotEmpty())
                                    createTopic(it.ownerName, it.name, it.diagramName)
                        }
                        is CreateFloatingTopic -> {
                            val location = Point2D.Double(it.location.first, it.location.second)
                            if (it.name.isNotEmpty() && it.diagramName.isNotEmpty())
                                createFloatingTopic(it.name, location, it.size, it.diagramName)
                        }
                        is ResizeClassPresentation -> {
                            val location = Point2D.Double(it.location.first, it.location.second)
                            if (it.className.isNotEmpty() && it.diagramName.isNotEmpty())
                                resizeClassPresentation(it.className, location, it.size, it.diagramName)
                        }
                        is ChangeOperationNameAndReturnTypeExpression -> {
                            if (it.ownerName.isNotEmpty()
                                && it.name.isNotEmpty()
                                && it.returnTypeExpression.isNotEmpty())
                                    changeOperationNameAndReturnTypeExpression(
                                        it.ownerName,
                                        it.brotherNameAndReturnTypeExpression,
                                        it.name,
                                        it.returnTypeExpression
                                    )
                        }
                        is ChangeAttributeNameAndTypeExpression -> {
                            if (it.ownerName.isNotEmpty()
                                && it.name.isNotEmpty()
                                && it.typeExpression.isNotEmpty())
                                changeAttributeNameAndTypeExpression(
                                    it.ownerName,
                                    it.brotherNameAndTypeExpression,
                                    it.name,
                                    it.typeExpression
                                )
                        }
                        is ResizeTopic -> {
                            val location = Point2D.Double(it.location.first, it.location.second)
                            if (it.name.isNotEmpty() && it.diagramName.isNotEmpty())
                                resizeTopic(it.name, location, it.size, it.diagramName)
                        }
                        is DeleteClassModel -> {
                            if (it.className.isNotEmpty())
                                deleteClassModel(it.className)
                        }
                        is DeleteAssociationPresentation -> {
                            val receivedPoints = it.points.map { point
                                -> Point2D.Double(point.first, point.second)
                            }.toList()
                            deleteAssociationPresentation(receivedPoints)
                        }
                        // TODO: DeleteAssociationModelを実装する
                    }
                }
                transactionManager.endTransaction()
                logger.debug("Finished transaction.")
                api.viewManager.diagramViewManager.select(emptyArray())
            } catch (e: BadTransactionException) {
                transactionManager.abortTransaction()
                logger.warn("Failed to transaction.", e)
                throw UnExpectedException()
            } finally {
                projectAccessor.addProjectEventListener(projectChangedListener)
            }
        }
    }

    private fun createClassDiagram(name: String, ownerName: String) {
        logger.debug("Create class diagram.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = classDiagramEditor.createClassDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    private fun createMindMapDiagram(name: String, ownerName: String) {
        logger.debug("Create mindmap diagram.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val mindmapEditor = diagramEditorFactory.mindmapEditor
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = mindmapEditor.createMindmapDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    private fun createClassModel(name: String, parentPackageName: String) {
        logger.debug("Create class model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val parentPackage = projectAccessor.findElements(IPackage::class.java, parentPackageName).first() as IPackage
        basicModelEditor.createClass(parentPackage, name)
    }

    private fun createAssociationModel(sourceClassName: String, destinationClassName: String, associationName: String) {
        logger.debug("Create association model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val destinationClass = projectAccessor.findElements(IClass::class.java, destinationClassName).first() as IClass
        basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
    }

    private fun createClassPresentation(className: String, location: Point2D, diagramName: String) {
        logger.debug("Create class presentation.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, className).first() as IClass
        classDiagramEditor.createNodePresentation(clazz, location)
    }

    private fun createAssociationPresentation(sourceClassName: String, targetClassName: String, diagramName: String) {
        logger.debug("Create association presentation.")
        @Throws(ClassNotFoundException::class)
        fun searchAssociation(sourceClass: IClass, targetClass: IClass): IAssociation {
            return sourceClass.attributes.first { sourceClassAttribute ->
                targetClass.attributes.any { targetClassAttribute ->
                    sourceClassAttribute.association == targetClassAttribute.association
                }
            }.association ?: throw ClassNotFoundException()
        }
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val sourceClassPresentation = sourceClass.presentations.first { it.diagram == diagram } as INodePresentation
        val targetClass = projectAccessor.findElements(IClass::class.java, targetClassName).first() as IClass
        val targetClassPresentation = targetClass.presentations.first { it.diagram == diagram } as INodePresentation
        logger.debug("Source: ${sourceClass.attributes}, target: ${targetClass.attributes}")
        try {
            val association = searchAssociation(sourceClass, targetClass)
            logger.debug("Association: $association")
            classDiagramEditor.createLinkPresentation(association, sourceClassPresentation, targetClassPresentation)
        } catch (e: ClassNotFoundException) {
            logger.error("Association not found.", e)
            return
        }
    }

    private fun createOperation(ownerName: String, name: String, returnTypeExpression: String) {
        logger.debug("Create operation.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        basicModelEditor.createOperation(ownerClass, name, returnTypeExpression)
    }

    private fun createAttribute(ownerName: String, name: String, typeExpression: String) {
        logger.debug("Create attribute.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        basicModelEditor.createAttribute(ownerClass, name, typeExpression)
    }

    @Throws(ClassNotFoundException::class)
    private fun searchTopic(name: String, topics: Array<INodePresentation>): INodePresentation {
        topics.forEach {
            if (it.label == name) return it
            else if (it.children.isNotEmpty()) {
                return searchTopic(name, it.children)
            }
        }
        throw ClassNotFoundException()
    }

    private fun createTopic(ownerName: String, name: String, diagramName: String) {
        logger.debug("Create topic.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val mindmapEditor = diagramEditorFactory.mindmapEditor
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
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val mindmapEditor = diagramEditorFactory.mindmapEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IMindMapDiagram
        mindmapEditor.diagram = diagram
        val parentTopic = diagram.root
        val topic = mindmapEditor.createTopic(parentTopic, name)
        mindmapEditor.changeToFloatingTopic(topic)
        topic.location = location
        topic.width = width
        topic.height = height
    }

    private fun resizeClassPresentation(className: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        logger.debug("Resize class presentation.")
        val (width, height) = size
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, className).first() as IClass
        val classPresentation = clazz.presentations.first { it.diagram == diagram } as INodePresentation
        classPresentation.location = location
        classPresentation.width = width
        classPresentation.height = height
    }

    private fun changeOperationNameAndReturnTypeExpression(ownerName: String, brotherNameAndReturnTypeExpression: List<Pair<String, String>>, name: String, returnTypeExpression: String) {
        logger.debug("Change operation name and return type expression.")
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        val operation = ownerClass.operations.filterNot {
            Pair(
                it.name,
                it.returnTypeExpression
            ) in brotherNameAndReturnTypeExpression
        }.first()
        operation.name = name
        operation.returnTypeExpression = returnTypeExpression
    }

    private fun changeAttributeNameAndTypeExpression(ownerName: String, brotherNameAndTypeExpression: List<Pair<String, String>>, name: String, typeExpression: String) {
        logger.debug("Change attribute name and type expression.")
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        val attribute = ownerClass.attributes.filterNot { Pair(it.name, it.typeExpression) in brotherNameAndTypeExpression }.first()
        attribute.name = name
        attribute.typeExpression = typeExpression
    }

    private fun resizeTopic(name: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        logger.debug("Resize topic.")
        val (width, height) = size
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val mindmapEditor = diagramEditorFactory.mindmapEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IMindMapDiagram
        mindmapEditor.diagram = diagram
        val topic = diagram.floatingTopics.first { it.label == name }
        topic.location = location
        topic.width = width
        topic.height = height
    }

    private fun deleteClassModel(name: String) {
        logger.debug("Delete class model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val clazz = projectAccessor.findElements(IClass::class.java, name).first() as IClass
        basicModelEditor.delete(clazz)
    }

    private fun deleteAssociationPresentation(receivedPoints: List<Point2D>) {
        logger.debug("Delete association presentation.")
        val foundLink = api.viewManager.diagramViewManager.currentDiagram.presentations
            .filterIsInstance<ILinkPresentation>().filter { entity -> entity.model is IAssociation }
            .firstOrNull { link ->
                link.points.all { receivedPoints.containsAll(link.points.toList()) }
            }
        foundLink?.let { link -> api.projectAccessor.modelEditorFactory.basicModelEditor.delete(link.model) }
    }

    companion object: Logging {
        private val logger = logger()
    }
}