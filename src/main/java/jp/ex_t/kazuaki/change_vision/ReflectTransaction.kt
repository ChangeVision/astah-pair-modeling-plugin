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
import com.change_vision.jude.api.inf.model.IAssociation
import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.model.IDiagram
import com.change_vision.jude.api.inf.model.IModel
import com.change_vision.jude.api.inf.presentation.INodePresentation
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import java.awt.geom.Point2D
import java.rmi.UnexpectedException

class ReflectTransaction(private val projectChangedListener: ProjectChangedListener? = null) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor

    @Throws(UnExpectedException::class)
    fun createClassModel(name: String, parentName: String,) {
        val transactionManager = projectAccessor.transactionManager
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val parent = projectAccessor.findElements(IModel::class.java, parentName).first() as IModel
        try {
            if (projectChangedListener != null)
                projectAccessor.removeProjectEventListener(projectChangedListener)
            transactionManager.beginTransaction()
            basicModelEditor.createClass(parent, name)
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
    fun createClassPresentation(className: String, location: Point2D, diagramName: String) {
        val transactionManager = projectAccessor.transactionManager
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, className).first() as IClass
        try {
            if (projectChangedListener != null)
                projectAccessor.removeProjectEventListener(projectChangedListener)
            transactionManager.beginTransaction()
            classDiagramEditor.createNodePresentation(clazz, location)
            transactionManager.endTransaction()
        } catch (e: BadTransactionException) {
            transactionManager.abortTransaction()
            throw UnExpectedException()
        } finally {
            if (projectChangedListener != null)
                projectAccessor.addProjectEventListener(projectChangedListener)
        }
    }

    @Throws(UnexpectedException::class)
    fun createAssociationModel(sourceClassName: String, destinationClassName: String, associationName: String) {
        val transactionManager = projectAccessor.transactionManager
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val destinationClass = projectAccessor.findElements(IClass::class.java, destinationClassName).first() as IClass
        try {
            if (projectChangedListener != null)
                projectAccessor.removeProjectEventListener(projectChangedListener)
            transactionManager.beginTransaction()
            basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
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
    fun addAssociationPresentation(sourceClassName: String, targetClassName: String, diagramName: String) {
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
        val transactionManager = projectAccessor.transactionManager
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
            try {
                if (projectChangedListener != null)
                    projectAccessor.removeProjectEventListener(projectChangedListener)
                transactionManager.beginTransaction()
                classDiagramEditor.createLinkPresentation(association, sourceClassPresentation, targetClassPresentation)
                transactionManager.endTransaction()
            } catch (e: BadTransactionException) {
                transactionManager.abortTransaction()
                throw UnExpectedException()
            } finally {
                if (projectChangedListener != null)
                    projectAccessor.addProjectEventListener(projectChangedListener)
            }
        } catch (e: ClassNotFoundException) {
            println("Association not found.")
            return
        }
    }
}