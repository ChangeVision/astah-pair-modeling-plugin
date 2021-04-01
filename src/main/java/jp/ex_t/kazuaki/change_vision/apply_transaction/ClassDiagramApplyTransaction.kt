/*
 * ClassDiagramApplyTransaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import java.awt.geom.Point2D

class ClassDiagramApplyTransaction: IApplyTransaction<ClassDiagramOperation> {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor

    @Throws(BadTransactionException::class)
    override fun apply(operations: List<ClassDiagramOperation>) {
        operations.forEach { it ->
            when (it) {
                is CreateClassDiagram -> {
                    if (it.name.isNotEmpty() && it.ownerName.isNotEmpty())
                        createClassDiagram(it.name, it.ownerName)
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
                is ResizeClassPresentation -> {
                    val location = Point2D.Double(it.location.first, it.location.second)
                    if (!operations.any { it is ChangeClassModelName }
                        && it.className.isNotEmpty() && it.diagramName.isNotEmpty())
                        resizeClassPresentation(it.className, location, it.size, it.diagramName)
                }
                is ChangeClassModelName -> {
                    if (it.name.isNotEmpty())
                        changeClassModelName(it.name, it.brotherClassNameList)
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
                is DeleteClassModel -> {
                    deleteClassModel(it.brotherClassNameList)
                }
                is DeleteClassPresentation -> {
                    if (it.className.isNotEmpty())
                        deleteClassPresentation(it.className)
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
            return (sourceClass.attributes.filterNot { it.association == null }.find { sourceClassAttribute ->
                targetClass.attributes.filterNot { it.association == null }.any { targetClassAttribute ->
                    sourceClassAttribute.association == targetClassAttribute.association
                }
            } ?: throw ClassNotFoundException()).association
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

    private fun resizeClassPresentation(className: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        logger.debug("Resize class presentation.")
        val (width, height) = size
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val clazz = (projectAccessor.findElements(IClass::class.java, className).first() ?: return) as IClass
        val classPresentation = clazz.presentations.first { it.diagram == diagram } as INodePresentation
        classPresentation.location = location
        classPresentation.width = width
        classPresentation.height = height
    }

    private fun changeClassModelName(name: String, brotherClassModelNameList: List<String?>) {
        logger.debug("Change class model name.")
        if (brotherClassModelNameList.isNullOrEmpty())
            projectAccessor.findElements(IClass::class.java).first().name = name
        else
            projectAccessor.findElements(IClass::class.java)
                .filterNot { it.name in brotherClassModelNameList }.first().name = name
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

    private fun deleteClassModel(brotherClassModelNameList: List<String?>) {
        logger.debug("Delete class model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val clazz = if (brotherClassModelNameList.isNullOrEmpty()) {
            projectAccessor.findElements(IClass::class.java).first()
        } else {
            projectAccessor.findElements(IClass::class.java)
                .filterNot { it.name in brotherClassModelNameList }.first()
        }
        basicModelEditor.delete(clazz)
    }

    private fun deleteClassPresentation(name: String) {
        logger.debug("Delete class model.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = api.viewManager.diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, name).first() as IClass
        val classPresentation = clazz.presentations.find { it.diagram == diagram } ?: return
        classDiagramEditor.deletePresentation(classPresentation)
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