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
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import java.awt.geom.Point2D
import javax.swing.SwingUtilities

class ReflectTransaction(private val projectChangedListener: ProjectChangedListener? = null) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor

    @Throws(UnExpectedException::class)
    fun transact(transaction: Transaction) {
        SwingUtilities.invokeLater {
            val transactionManager = projectAccessor.transactionManager
            try {
                if (projectChangedListener != null)
                    projectAccessor.removeProjectEventListener(projectChangedListener)
                transactionManager.beginTransaction()
                if (transaction.createClassDiagram != null) {
                    val createClassDiagram = transaction.createClassDiagram as CreateClassDiagram
                    val name = createClassDiagram.name
                    val ownerName = createClassDiagram.ownerName
                    if (name.isNotEmpty() && ownerName.isNotEmpty())
                        createClassDiagram(name, ownerName)
                }
                if (transaction.createMindMapDiagram != null) {
                    val createMindMapDiagram = transaction.createMindMapDiagram as CreateMindMapDiagram
                    val name = createMindMapDiagram.name
                    val ownerName = createMindMapDiagram.ownerName
                    if (name.isNotEmpty() && ownerName.isNotEmpty())
                        createMindMapDiagram(name, ownerName)
                }
                if (transaction.createClassModel != null) {
                    val createClassModel = transaction.createClassModel as CreateClassModel
                    val name = createClassModel.name
                    val parentPackageName = createClassModel.parentPackageName
                    if (parentPackageName.isNotEmpty() && name.isNotEmpty())
                        createClassModel(name, parentPackageName)
                }
                if (transaction.createAssociationModel != null) {
                    val createAssociationModel = transaction.createAssociationModel as CreateAssociationModel
                    val sourceClassName = createAssociationModel.sourceClassName
                    val destinationClassName = createAssociationModel.destinationClassName
                    val name = createAssociationModel.name
                    if (sourceClassName.isNotEmpty() && destinationClassName.isNotEmpty())
                        createAssociationModel(sourceClassName, destinationClassName, name)
                }
                if (transaction.createClassPresentation != null) {
                    val createClassPresentation = transaction.createClassPresentation as CreateClassPresentation
                    val className = createClassPresentation.className
                    val locationPair = createClassPresentation.location
                    val location = Point2D.Double(locationPair.first, locationPair.second)
                    val diagramName = createClassPresentation.diagramName
                    if (diagramName.isNotEmpty() && className.isNotEmpty())
                        createClassPresentation(className, location, diagramName)
                }
                if (transaction.createAssociationPresentation != null) {
                    val createAssociationPresentation = transaction.createAssociationPresentation as CreateAssociationPresentation
                    val sourceClassName = createAssociationPresentation.sourceClassName
                    val targetClassName = createAssociationPresentation.targetClassName
                    val diagramName = createAssociationPresentation.diagramName
                    if (sourceClassName.isNotEmpty() && targetClassName.isNotEmpty() && diagramName.isNotEmpty())
                        createAssociationPresentation(sourceClassName, targetClassName, diagramName)
                }
                if (transaction.createAttribute != null) {
                    val createAttribute = transaction.createAttribute as CreateAttribute
                    val ownerName = createAttribute.ownerName
                    val name = createAttribute.name
                    val typeExpression = createAttribute.typeExpression
                    if (ownerName.isNotEmpty() && name.isNotEmpty() && typeExpression.isNotEmpty())
                        createAttribute(ownerName, name, typeExpression)
                }
                if (transaction.createTopic != null) {
                    val createTopic = transaction.createTopic as CreateTopic
                    val ownerName = createTopic.ownerName
                    val name = createTopic.name
                    val diagramName = createTopic.diagramName
                    if (ownerName.isNotEmpty() && name.isNotEmpty() && diagramName.isNotEmpty())
                        createTopic(ownerName, name, diagramName)
                }
                if (transaction.createFloatingTopic != null) {
                    val createFloatingTopic = transaction.createFloatingTopic as CreateFloatingTopic
                    val name = createFloatingTopic.name
                    val locationPair = createFloatingTopic.location
                    val location = Point2D.Double(locationPair.first, locationPair.second)
                    val size =createFloatingTopic.size
                    val diagramName = createFloatingTopic.diagramName
                    if (name.isNotEmpty() && diagramName.isNotEmpty())
                        createFloatingTopic(name, location, size, diagramName)
                }
                if (transaction.resizeClassPresentation != null) {
                    val resizeClassPresentation = transaction.resizeClassPresentation as ResizeClassPresentation
                    val className = resizeClassPresentation.className
                    val locationPair = resizeClassPresentation.location
                    val location = Point2D.Double(locationPair.first, locationPair.second)
                    val size = resizeClassPresentation.size
                    val diagramName = resizeClassPresentation.diagramName
                    if (className.isNotEmpty() && diagramName.isNotEmpty())
                        resizeClassPresentation(className, location, size, diagramName)
                }
                if (transaction.changeAttributeNameAndTypeExpression != null) {
                    val changeAttributeNameAndTypeExpression = transaction.changeAttributeNameAndTypeExpression as ChangeAttributeNameAndTypeExpression
                    val ownerName = changeAttributeNameAndTypeExpression.ownerName
                    val brotherNameAndTypeExpression = changeAttributeNameAndTypeExpression.brotherNameAndTypeExpression
                    val name = changeAttributeNameAndTypeExpression.name
                    val typeExpression = changeAttributeNameAndTypeExpression.typeExpression
                    if (ownerName.isNotEmpty() && name.isNotEmpty() && typeExpression.isNotEmpty())
                        changeAttributeNameAndTypeExpression(ownerName, brotherNameAndTypeExpression, name, typeExpression)
                }
                if (transaction.resizeTopic != null) {
                    val resizeTopic = transaction.resizeTopic as ResizeTopic
                    val name = resizeTopic.name
                    val locationPair = resizeTopic.location
                    val location = Point2D.Double(locationPair.first, locationPair.second)
                    val size = resizeTopic.size
                    val diagramName = resizeTopic.diagramName
                    if (name.isNotEmpty() && diagramName.isNotEmpty())
                        resizeTopic(name, location, size, diagramName)
                }
                if (transaction.deleteClassModel != null) {
                    val deleteClassModel = transaction.deleteClassModel as DeleteClassModel
                    val name = deleteClassModel.className
                    if (name.isNotEmpty())
                        deleteClassModel(name)
                }
                transactionManager.endTransaction()
            } catch (e: BadTransactionException) {
                transactionManager.abortTransaction()
                throw UnExpectedException()
            } finally {
                if (projectChangedListener != null)
                    projectAccessor.addProjectEventListener(projectChangedListener)
            }
        }
    }

    private fun createClassDiagram(name: String, ownerName: String) {
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = classDiagramEditor.createClassDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    private fun createMindMapDiagram(name: String, ownerName: String) {
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val mindmapEditor = diagramEditorFactory.mindmapEditor
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = mindmapEditor.createMindmapDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    private fun createClassModel(name: String, parentPackageName: String) {
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val parentPackage = projectAccessor.findElements(IPackage::class.java, parentPackageName).first() as IPackage
        basicModelEditor.createClass(parentPackage, name)
    }

    private fun createAssociationModel(sourceClassName: String, destinationClassName: String, associationName: String) {
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val destinationClass = projectAccessor.findElements(IClass::class.java, destinationClassName).first() as IClass
        basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
    }

    private fun createClassPresentation(className: String, location: Point2D, diagramName: String) {
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, className).first() as IClass
        classDiagramEditor.createNodePresentation(clazz, location)
    }

    private fun createAssociationPresentation(sourceClassName: String, targetClassName: String, diagramName: String) {
        @Throws(ClassNotFoundException::class)
        fun searchAssociation(sourceClass: IClass, targetClass: IClass): IAssociation? {
            sourceClass.attributes.forEach { sourceClassAttribute ->
                targetClass.attributes.forEach {
                    if (sourceClassAttribute.association == it.association)
                        return it.association
                }
            }
            throw ClassNotFoundException()
        }
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val sourceClassPresentation = sourceClass.presentations.first { it.diagram == diagram } as INodePresentation
        val targetClass = projectAccessor.findElements(IClass::class.java, targetClassName).first() as IClass
        val targetClassPresentation = targetClass.presentations.first { it.diagram == diagram } as INodePresentation
        try {
            val association = searchAssociation(sourceClass, targetClass)
            classDiagramEditor.createLinkPresentation(association, sourceClassPresentation, targetClassPresentation)
        } catch (e: ClassNotFoundException) {
            println("Association not found.")
            return
        }
    }

    private fun createAttribute(ownerName: String, name: String, typeExpression: String) {
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        basicModelEditor.createAttribute(ownerClass, name, typeExpression)
    }

    private fun searchTopic(name: String, topics: Array<INodePresentation>): INodePresentation? {
        topics.forEach {
            if (it.label == name) return it
            else if (it.children.isNotEmpty()) {
                return searchTopic(name, it.children)
            }
        }
        return null
    }

    private fun createTopic(ownerName: String, name: String, diagramName: String) {
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val mindmapEditor = diagramEditorFactory.mindmapEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IMindMapDiagram
        mindmapEditor.diagram = diagram
        val topics = diagram.floatingTopics + diagram.root
        val parent = searchTopic(ownerName, topics) ?: return
        mindmapEditor.createTopic(parent, name)
    }

    private fun createFloatingTopic(name: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
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

    private fun changeAttributeNameAndTypeExpression(ownerName: String, brotherNameAndTypeExpression: List<Pair<String, String>>, name: String, typeExpression: String) {
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        val attribute = ownerClass.attributes.filterNot { Pair(it.name, it.typeExpression) in brotherNameAndTypeExpression }.first()
        attribute.name = name
        attribute.typeExpression = typeExpression
    }

    private fun resizeTopic(name: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
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
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val clazz = projectAccessor.findElements(IClass::class.java, name).first() as IClass
        basicModelEditor.delete(clazz)
    }
}