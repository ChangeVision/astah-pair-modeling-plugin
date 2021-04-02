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
                    if (it.name.isNotEmpty() && it.ownerName.isNotEmpty()) {
                        createClassDiagram(it.name, it.ownerName)
                    }
                }
                is CreateClassModel -> {
                    if (it.name.isNotEmpty() && it.parentPackageName.isNotEmpty()) {
                        createClassModel(it.name, it.parentPackageName, it.stereotypes)
                    }
                }
                is CreateAssociationModel -> {
                    if (it.sourceClassName.isNotEmpty()
                        && it.sourceClassNavigability.isNotEmpty()
                        && it.destinationClassName.isNotEmpty()
                        && it.destinationClassNavigability.isNotEmpty()) {
                        createAssociationModel(
                            it.sourceClassName,
                            it.sourceClassNavigability,
                            it.destinationClassName,
                            it.destinationClassNavigability,
                            it.name,
                        )
                    }
                }
                is CreateGeneralizationModel -> {
                    if (it.superClassName.isNotEmpty()
                        && it.subClassName.isNotEmpty()) {
                        createGeneralizationModel(
                            it.superClassName,
                            it.subClassName,
                            it.name,
                        )
                    }
                }
                is CreateRealizationModel -> {
                    if (it.supplierClassName.isNotEmpty()
                        && it.clientClassName.isNotEmpty()) {
                        createRealizationModel(
                            it.supplierClassName,
                            it.clientClassName,
                            it.name,
                        )
                    }
                }
                is CreateClassPresentation -> {
                    val location = Point2D.Double(it.location.first, it.location.second)
                    if (it.diagramName.isNotEmpty() && it.className.isNotEmpty()) {
                        createClassPresentation(it.className, location, it.diagramName)
                    }
                }
                is CreateLinkPresentation -> {
                    if (it.sourceClassName.isNotEmpty()
                        && it.targetClassName.isNotEmpty()
                        && it.linkType.isNotEmpty()
                        && it.diagramName.isNotEmpty()) {
                        createLinkPresentation(
                            it.sourceClassName,
                            it.targetClassName,
                            it.linkType,
                            it.diagramName
                        )
                    }
                }
                is CreateOperation -> {
                    if (it.ownerName.isNotEmpty()
                        && it.name.isNotEmpty()
                        && it.returnTypeExpression.isNotEmpty()) {
                        createOperation(it.ownerName, it.name, it.returnTypeExpression)
                    }
                }
                is CreateAttribute -> {
                    if (it.ownerName.isNotEmpty()
                        && it.name.isNotEmpty()
                        && it.typeExpression.isNotEmpty()) {
                        createAttribute(it.ownerName, it.name, it.typeExpression)
                    }
                }
                is ResizeClassPresentation -> {
                    val location = Point2D.Double(it.location.first, it.location.second)
                    if (!operations.any { it is ChangeClassModel }
                        && it.className.isNotEmpty() && it.diagramName.isNotEmpty()) {
                        resizeClassPresentation(it.className, location, it.size, it.diagramName)
                    }
                }
                is ChangeClassModel -> {
                    if (it.name.isNotEmpty()) {
                        changeClassModel(it.name, it.brotherClassNameList, it.stereotypes)
                    }
                }
                is ChangeOperationNameAndReturnTypeExpression -> {
                    if (it.ownerName.isNotEmpty()
                        && it.name.isNotEmpty()
                        && it.returnTypeExpression.isNotEmpty()) {
                        changeOperationNameAndReturnTypeExpression(
                            it.ownerName,
                            it.brotherNameAndReturnTypeExpression,
                            it.name,
                            it.returnTypeExpression
                        )
                    }
                }
                is ChangeAttributeNameAndTypeExpression -> {
                    if (it.ownerName.isNotEmpty()
                        && it.name.isNotEmpty()
                        && it.typeExpression.isNotEmpty()) {
                        changeAttributeNameAndTypeExpression(
                            it.ownerName,
                            it.brotherNameAndTypeExpression,
                            it.name,
                            it.typeExpression
                        )
                    }
                }
                is DeleteClassModel -> {
                    deleteClassModel(it.brotherClassNameList)
                }
                is DeleteAssociationModel -> {
                    val deleteAssociationPresentation = (operations.find {
                        it is DeleteLinkPresentation
                    } ?: return) as DeleteLinkPresentation
                    val receivedPoints = deleteAssociationPresentation.points.map { point
                        -> Point2D.Double(point.first, point.second)
                    }.toList()
                    deleteAssociationModel(receivedPoints)
                }
                is DeleteClassPresentation -> {
                    if (it.className.isNotEmpty()) {
                        deleteClassPresentation(it.className)
                    }
                }
                is DeleteLinkPresentation -> {
                    if (!operations.any { it is DeleteAssociationModel }) {
                        val receivedPoints = it.points.map { point
                            -> Point2D.Double(point.first, point.second)
                        }.toList()
                        if (it.linkType.isNotEmpty()) {
                            deleteLinkPresentation(receivedPoints, it.linkType)
                        }
                    }
                }
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

    private fun createClassModel(name: String, parentPackageName: String, stereotypes: List<String?>) {
        logger.debug("Create class model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val parentPackage = projectAccessor.findElements(IPackage::class.java, parentPackageName).first() as IPackage
        val clazz = basicModelEditor.createClass(parentPackage, name)
        stereotypes.forEach {
            clazz.addStereotype(it)
        }
    }

    private fun createAssociationModel(sourceClassName: String, sourceClassNavigability: String, destinationClassName: String, destinationClassNavigability: String, associationName: String) {
        logger.debug("Create association model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val destinationClass = projectAccessor.findElements(IClass::class.java, destinationClassName).first() as IClass
        val association = basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
        sourceClass.attributes.filterNot { it.association == null }.find { it.association == association }?.navigability = sourceClassNavigability
        destinationClass.attributes.filterNot { it.association == null }.find { it.association == association }?.navigability = destinationClassNavigability
    }

    private fun createGeneralizationModel(superClassName: String, subClassName: String, name: String) {
        logger.debug("Create generalization model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val superClass = projectAccessor.findElements(IClass::class.java, superClassName).first() as IClass
        val subClass = projectAccessor.findElements(IClass::class.java, subClassName).first() as IClass
        basicModelEditor.createGeneralization(subClass, superClass, name)
    }

    private fun createRealizationModel(supplierClassName: String, clientClassName: String, name: String) {
        logger.debug("Create realization model.")
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val supplierClass = projectAccessor.findElements(IClass::class.java, supplierClassName).first() as IClass
        val clientClass = projectAccessor.findElements(IClass::class.java, clientClassName).first() as IClass
        basicModelEditor.createRealization(clientClass, supplierClass, name)
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

    private fun createLinkPresentation(sourceClassName: String, targetClassName: String, linkType: String, diagramName: String) {
        logger.debug("Create link presentation.")
        @Throws(ClassNotFoundException::class)
        fun searchModel(linkType: String, sourceClass: IClass, targetClass: IClass): IElement {
            when (linkType) {
                "Association" -> {
                    return (sourceClass.attributes.filterNot { it.association == null }.find { sourceClassAttribute ->
                        targetClass.attributes.filterNot { it.association == null }.any { targetClassAttribute ->
                            sourceClassAttribute.association == targetClassAttribute.association
                        }
                    } ?: throw ClassNotFoundException()).association
                }
                "Generalization" -> {
                    return sourceClass.generalizations.find {
                        it.superType == targetClass
                    } ?: throw ClassNotFoundException()
                }
                "Realization" -> {
                    return sourceClass.supplierRealizations.find {
                        it.client == targetClass
                    } ?: throw ClassNotFoundException()
                }
                else -> {
                    throw NotImplementedError()
                }
            }
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
            val element = searchModel(linkType, sourceClass, targetClass)
            logger.debug("Link: $element")
            classDiagramEditor.createLinkPresentation(element, sourceClassPresentation, targetClassPresentation)
        } catch (e: ClassNotFoundException) {
            logger.error("Link model not found.", e)
            return
        } catch (e: NotImplementedError) {
            logger.error("Not implemented.", e)
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

    private fun changeClassModel(name: String, brotherClassModelNameList: List<String?>, stereotypes: List<String?>) {
        logger.debug("Change class model name and stereotypes.")
        val clazz = (
                if (brotherClassModelNameList.isNullOrEmpty())
                    projectAccessor.findElements(IClass::class.java).first()
                else projectAccessor.findElements(IClass::class.java)
                    .filterNot { it.name in brotherClassModelNameList }.first()
                ) as IClass
        clazz.name = name
        val clazzStereotypes = clazz.stereotypes.toList()
        logger.debug("Add stereotypes.")
        stereotypes.filterNot { it in clazzStereotypes }.forEach {
            clazz.addStereotype(it)
        }
        logger.debug("Remove stereotypes.")
        clazzStereotypes.filterNot { it in stereotypes }.forEach {
            clazz.removeStereotype(it)
        }
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

    private fun searchLinkPresentation(presentations: List<ILinkPresentation>, receivedPoints: List<Point2D>, linkType: String): ILinkPresentation? {
        return when (linkType) {
            "Association" -> {
                presentations.filter { entity -> entity.model is IAssociation }
                    .firstOrNull { link -> link.points.all { receivedPoints.containsAll(link.points.toList()) } }
            }
            else -> {
                null
            }
        }
    }

    private fun deleteAssociationModel(receivedPoints: List<Point2D>) {
        logger.debug("Delete association model.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = api.viewManager.diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val foundLink = diagram.presentations
            .filterIsInstance<ILinkPresentation>().filter { entity -> entity.model is IAssociation }
            .firstOrNull { link ->
                link.points.all { receivedPoints.containsAll(link.points.toList()) }
            }
        foundLink?.let { projectAccessor.modelEditorFactory.basicModelEditor.delete(it.model) }
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

    private fun deleteLinkPresentation(receivedPoints: List<Point2D>, linkType: String) {
        logger.debug("Delete association presentation.")
        val diagramEditorFactory = projectAccessor.diagramEditorFactory
        val classDiagramEditor = diagramEditorFactory.classDiagramEditor
        val diagram = api.viewManager.diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val foundLink = searchLinkPresentation(
            diagram.presentations.filterIsInstance<ILinkPresentation>(),
            receivedPoints,
            linkType
        )
        foundLink?.let { classDiagramEditor.deletePresentation(it) }
    }

    companion object: Logging {
        private val logger = logger()
    }
}