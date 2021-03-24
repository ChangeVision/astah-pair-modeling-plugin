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
import java.rmi.UnexpectedException

class ReflectTransaction(private val projectChangedListener: ProjectChangedListener? = null) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor

    @Throws(UnExpectedException::class)
    fun transact(transaction: Transaction) {
        val transactionManager = projectAccessor.transactionManager
        try {
            if (projectChangedListener != null)
                projectAccessor.removeProjectEventListener(projectChangedListener)
            transactionManager.beginTransaction()
            if (transaction.createClassDiagram != null) {
                val createClassDiagram = transaction.createClassDiagram as CreateClassDiagram
                val name = createClassDiagram.name
                val ownerName = createClassDiagram.ownerName
                if (name.isEmpty() || ownerName.isEmpty()) return
                createClassDiagram(name, ownerName)
            }
            if (transaction.createClassModel != null) {
                val createClassModel = transaction.createClassModel as CreateClassModel
                val name = createClassModel.name
                val parentPackageName = createClassModel.parentPackageName
                if (parentPackageName.isEmpty() || name.isEmpty()) return
                createClassModel(name, parentPackageName)
            }
            if (transaction.createClassPresentation != null) {
                val createClassPresentation = transaction.createClassPresentation as CreateClassPresentation
                val className = createClassPresentation.className
                val locationPair = createClassPresentation.location
                val location = Point2D.Double(locationPair.first, locationPair.second)
                val diagramName = createClassPresentation.diagramName
                if (diagramName.isEmpty() || className.isEmpty()) return
                createClassPresentation(className, location, diagramName)
            }
            if (transaction.resizeClassPresentation != null) {
                val resizeClassPresentation = transaction.resizeClassPresentation as ResizeClassPresentation
                val className = resizeClassPresentation.className
                val locationPair = resizeClassPresentation.location
                val location = Point2D.Double(locationPair.first, locationPair.second)
                val size = resizeClassPresentation.size
                val diagramName = resizeClassPresentation.diagramName
                if (className.isEmpty() || diagramName.isEmpty()) return
                resizeClassPresentation(className, location, size, diagramName)
            }
            if (transaction.createAssociationModel != null) {
                val createAssociationModel = transaction.createAssociationModel as CreateAssociationModel
                val sourceClassName = createAssociationModel.sourceClassName
                val destinationClassName = createAssociationModel.destinationClassName
                val name = createAssociationModel.name
                if (sourceClassName.isEmpty() || destinationClassName.isEmpty()) return
                createAssociationModel(sourceClassName, destinationClassName, name)
            }
            if (transaction.createAssociationPresentation != null) {
                val createAssociationPresentation = transaction.createAssociationPresentation as CreateAssociationPresentation
                val sourceClassName = createAssociationPresentation.sourceClassName
                val targetClassName = createAssociationPresentation.targetClassName
                val diagramName = createAssociationPresentation.diagramName
                if (sourceClassName.isEmpty() || targetClassName.isEmpty() || diagramName.isEmpty()) return
                createAssociationPresentation(sourceClassName, targetClassName, diagramName)
            }
            if (transaction.deleteClassModel != null) {
                val deleteClassModel = transaction.deleteClassModel as DeleteClassModel
                val name = deleteClassModel.className
                if (name.isEmpty()) return
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

    @Throws(UnExpectedException::class)
    fun createClassDiagram(name: String, ownerName: String) {
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagramViewManager = api.viewManager.diagramViewManager
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = classDiagramEditor.createClassDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    @Throws(UnExpectedException::class)
    fun createClassModel(name: String, parentPackageName: String) {
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val parentPackage = projectAccessor.findElements(IPackage::class.java, parentPackageName).first() as IPackage
        basicModelEditor.createClass(parentPackage, name)
    }

    @Throws(UnExpectedException::class)
    fun createClassPresentation(className: String, location: Point2D, diagramName: String) {
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, className).first() as IClass
        classDiagramEditor.createNodePresentation(clazz, location)
    }

    @Throws(UnExpectedException::class)
    fun resizeClassPresentation(className: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        val (width, height) = size
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, className).first() as IClass
        val classPresentation = clazz.presentations.first { it.diagram == diagram } as INodePresentation
        classPresentation.location = location
        classPresentation.width = width
        classPresentation.height =height
    }

    @Throws(UnexpectedException::class)
    fun createAssociationModel(sourceClassName: String, destinationClassName: String, associationName: String) {
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val destinationClass = projectAccessor.findElements(IClass::class.java, destinationClassName).first() as IClass
        basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
    }

    @Throws(UnExpectedException::class)
    fun createAssociationPresentation(sourceClassName: String, targetClassName: String, diagramName: String) {
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

    @Throws(UnExpectedException::class)
    fun deleteClassModel(name: String) {
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val clazz = projectAccessor.findElements(IClass::class.java, name).first() as IClass
        basicModelEditor.delete(clazz)
    }
}