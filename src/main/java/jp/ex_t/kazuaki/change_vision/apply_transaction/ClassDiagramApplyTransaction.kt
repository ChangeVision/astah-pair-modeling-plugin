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
    private val diagramViewManager = api.viewManager.diagramViewManager
    private val classDiagramEditor = projectAccessor.diagramEditorFactory.classDiagramEditor
    private val basicModelEditor = projectAccessor.modelEditorFactory.basicModelEditor

    @Throws(BadTransactionException::class)
    override fun apply(operations: List<ClassDiagramOperation>) {
        operations.forEach { it ->
            when (it) {
                is CreateClassDiagram -> {
                    validateAndCreateClassDiagram(it)
                }
                is CreateClassModel -> {
                    validateAndCreateClassModel(it)
                }
                is CreateAssociationModel -> {
                    validateAndCreateAssociationModel(it)
                }
                is CreateGeneralizationModel -> {
                    validateAndCreateGeneralizationModel(it)
                }
                is CreateRealizationModel -> {
                    validateAndCreateRealizationModel(it)
                }
                is CreateClassPresentation -> {
                    validateAndCreateClassPresentation(it)
                }
                is CreateLinkPresentation -> {
                    validateAndCreateLinkPresentation(it)
                }
                is CreateOperation -> {
                    validateAndCreateOperation(it)
                }
                is CreateAttribute -> {
                    validateAndCreateAttribute(it)
                }
                is ResizeClassPresentation -> {
                    validateAndResizeClassPresentation(it, operations)
                }
                is ChangeClassModel -> {
                    validateAndChangeClassModel(it)
                }
                is ChangeOperationNameAndReturnTypeExpression -> {
                    validateAndChangeOperationNameAndReturnTypeExpression(it)
                }
                is ChangeAttributeNameAndTypeExpression -> {
                    validateAndChangeAttributeNameAndTypeExpression(it)
                }
                is DeleteClassModel -> {
                    validateAndDeleteClassModel(it)
                }
                is DeleteLinkModel -> {
                    deleteLinkModel(operations)
                }
                is DeleteClassPresentation -> {
                    validateAndDeleteClassPresentation(it)
                }
                is DeleteLinkPresentation -> {
                    validateAndDeleteLinkPresentation(it, operations)
                }
            }
        }
    }

    private fun validateAndCreateClassDiagram(operation: CreateClassDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty()) {
            createClassDiagram(operation.name, operation.ownerName)
        }
    }

    private fun validateAndCreateClassModel(operation: CreateClassModel) {
        if (operation.name.isNotEmpty() && operation.parentPackageName.isNotEmpty()) {
            createClassModel(operation.name, operation.parentPackageName, operation.stereotypes)
        }
    }

    private fun validateAndCreateAssociationModel(operation: CreateAssociationModel) {
        if (operation.sourceClassName.isNotEmpty()
            && operation.sourceClassNavigability.isNotEmpty()
            && operation.destinationClassName.isNotEmpty()
            && operation.destinationClassNavigability.isNotEmpty()) {
                createAssociationModel(
                    operation.sourceClassName,
                    operation.sourceClassNavigability,
                    operation.destinationClassName,
                    operation.destinationClassNavigability,
                    operation.name,
                )
        }
    }

    private fun validateAndCreateGeneralizationModel(operation: CreateGeneralizationModel) {
        if (operation.superClassName.isNotEmpty()
            && operation.subClassName.isNotEmpty()) {
                createGeneralizationModel(
                    operation.superClassName,
                    operation.subClassName,
                    operation.name,
                )
        }
    }

    private fun validateAndCreateRealizationModel(operation: CreateRealizationModel) {
        if (operation.supplierClassName.isNotEmpty()
            && operation.clientClassName.isNotEmpty()) {
                createRealizationModel(
                    operation.supplierClassName,
                    operation.clientClassName,
                    operation.name,
                )
        }
    }

    private fun validateAndCreateClassPresentation(operation: CreateClassPresentation) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.diagramName.isNotEmpty() && operation.className.isNotEmpty()) {
            createClassPresentation(operation.className, location, operation.diagramName)
        }
    }

    private fun validateAndCreateLinkPresentation(operation: CreateLinkPresentation) {
        if (operation.sourceClassName.isNotEmpty()
            && operation.targetClassName.isNotEmpty()
            && operation.linkType.isNotEmpty()
            && operation.diagramName.isNotEmpty()) {
                createLinkPresentation(
                    operation.sourceClassName,
                    operation.targetClassName,
                    operation.linkType,
                    operation.diagramName
                )
        }
    }

    private fun validateAndCreateOperation(operation: CreateOperation) {
        if (operation.ownerName.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.returnTypeExpression.isNotEmpty()) {
                createOperation(
                    operation.ownerName,
                    operation.name,
                    operation.returnTypeExpression
                )
        }
    }

    private fun validateAndCreateAttribute(operation: CreateAttribute) {
        if (operation.ownerName.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.typeExpression.isNotEmpty()) {
                createAttribute(operation.ownerName, operation.name, operation.typeExpression)
        }
    }

    private fun validateAndResizeClassPresentation(operation: ResizeClassPresentation, operations: List<ClassDiagramOperation>) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (!operations.any { it is ChangeClassModel }
            && operation.className.isNotEmpty()
            && operation.diagramName.isNotEmpty()) {
                resizeClassPresentation(
                    operation.className,
                    location,
                    operation.size,
                    operation.diagramName
                )
        }
    }

    private fun validateAndChangeClassModel(operation: ChangeClassModel) {
        if (operation.name.isNotEmpty()) {
            changeClassModel(operation.name, operation.brotherClassNameList, operation.stereotypes)
        }
    }

    private fun validateAndChangeOperationNameAndReturnTypeExpression(operation: ChangeOperationNameAndReturnTypeExpression) {
        if (operation.ownerName.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.returnTypeExpression.isNotEmpty()) {
                changeOperationNameAndReturnTypeExpression(
                    operation.ownerName,
                    operation.brotherNameAndReturnTypeExpression,
                    operation.name,
                    operation.returnTypeExpression
                )
        }
    }

    private fun validateAndChangeAttributeNameAndTypeExpression(operation: ChangeAttributeNameAndTypeExpression) {
        if (operation.ownerName.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.typeExpression.isNotEmpty()) {
                changeAttributeNameAndTypeExpression(
                    operation.ownerName,
                    operation.brotherNameAndTypeExpression,
                    operation.name,
                    operation.typeExpression
                )
        }
    }

    private fun validateAndDeleteClassModel(operation: DeleteClassModel) {
        deleteClassModel(operation.brotherClassNameList)
    }

    private fun deleteLinkModel(operations: List<ClassDiagramOperation>) {
        val deleteLinkPresentation = (operations.find {
            it is DeleteLinkPresentation
        } ?: return) as DeleteLinkPresentation
        val receivedPoints = deleteLinkPresentation.points.map { point
            -> Point2D.Double(point.first, point.second)
        }.toList()
        if (deleteLinkPresentation.linkType.isNotEmpty()) {
            deleteLinkModel(receivedPoints, deleteLinkPresentation.linkType)
        }
    }

    private fun validateAndDeleteClassPresentation(operation: DeleteClassPresentation) {
        if (operation.className.isNotEmpty()) {
            deleteClassPresentation(operation.className)
        }
    }

    private fun validateAndDeleteLinkPresentation(operation: DeleteLinkPresentation, operations: List<ClassDiagramOperation>) {
        if (!operations.any { it is DeleteLinkModel }) {
            val receivedPoints = operation.points.map { point
                -> Point2D.Double(point.first, point.second)
            }.toList()
            if (operation.linkType.isNotEmpty()) {
                deleteLinkPresentation(receivedPoints, operation.linkType)
            }
        }
    }

    private fun createClassDiagram(name: String, ownerName: String) {
        logger.debug("Create class diagram.")
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = classDiagramEditor.createClassDiagram(owner, name)
        diagramViewManager.open(diagram)
    }

    private fun createClassModel(name: String, parentPackageName: String, stereotypes: List<String?>) {
        logger.debug("Create class model.")
        val parentPackage = projectAccessor.findElements(IPackage::class.java, parentPackageName).first() as IPackage
        val clazz = basicModelEditor.createClass(parentPackage, name)
        stereotypes.forEach {
            clazz.addStereotype(it)
        }
    }

    private fun createAssociationModel(sourceClassName: String, sourceClassNavigability: String, destinationClassName: String, destinationClassNavigability: String, associationName: String) {
        logger.debug("Create association model.")
        val sourceClass = projectAccessor.findElements(IClass::class.java, sourceClassName).first() as IClass
        val destinationClass = projectAccessor.findElements(IClass::class.java, destinationClassName).first() as IClass
        val association = basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
        sourceClass.attributes.filterNot { it.association == null }.find { it.association == association }?.navigability = sourceClassNavigability
        destinationClass.attributes.filterNot { it.association == null }.find { it.association == association }?.navigability = destinationClassNavigability
    }

    private fun createGeneralizationModel(superClassName: String, subClassName: String, name: String) {
        logger.debug("Create generalization model.")
        val superClass = projectAccessor.findElements(IClass::class.java, superClassName).first() as IClass
        val subClass = projectAccessor.findElements(IClass::class.java, subClassName).first() as IClass
        basicModelEditor.createGeneralization(subClass, superClass, name)
    }

    private fun createRealizationModel(supplierClassName: String, clientClassName: String, name: String) {
        logger.debug("Create realization model.")
        val supplierClass = projectAccessor.findElements(IClass::class.java, supplierClassName).first() as IClass
        val clientClass = projectAccessor.findElements(IClass::class.java, clientClassName).first() as IClass
        basicModelEditor.createRealization(clientClass, supplierClass, name)
    }

    private fun createClassPresentation(className: String, location: Point2D, diagramName: String) {
        logger.debug("Create class presentation.")
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
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        basicModelEditor.createOperation(ownerClass, name, returnTypeExpression)
    }

    private fun createAttribute(ownerName: String, name: String, typeExpression: String) {
        logger.debug("Create attribute.")
        val ownerClass = projectAccessor.findElements(IClass::class.java, ownerName).first() as IClass
        basicModelEditor.createAttribute(ownerClass, name, typeExpression)
    }

    private fun resizeClassPresentation(className: String, location: Point2D, size: Pair<Double, Double>, diagramName: String) {
        logger.debug("Resize class presentation.")
        val (width, height) = size
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
                logger.debug("Search association link presentation.")
                presentations.filter { entity -> entity.model is IAssociation }
                    .firstOrNull { link -> link.points.all { receivedPoints.containsAll(link.points.toList()) } }
            }
            "Generalization" -> {
                logger.debug("Search generalization link presentation.")
                presentations.filter { entity -> entity.model is IGeneralization }
                    .firstOrNull { link -> link.points.all { receivedPoints.containsAll(link.points.toList()) } }
            }
            "Realization" -> {
                logger.debug("Search realization link presentation.")
                presentations.filter { entity -> entity.model is IRealization }
                    .firstOrNull { link -> link.points.all { receivedPoints.containsAll(link.points.toList()) } }
            }
            else -> {
                null
            }
        }
    }

    private fun deleteLinkModel(receivedPoints: List<Point2D>, linkType: String) {
        logger.debug("Delete link model.")
        val diagram = diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val foundLink = searchLinkPresentation(
            diagram.presentations.filterIsInstance<ILinkPresentation>(),
            receivedPoints,
            linkType
        )
        foundLink?.let { projectAccessor.modelEditorFactory.basicModelEditor.delete(it.model) }
    }

    private fun deleteClassPresentation(name: String) {
        logger.debug("Delete class model.")
        val diagram = diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val clazz = projectAccessor.findElements(IClass::class.java, name).first() as IClass
        val classPresentation = clazz.presentations.find { it.diagram == diagram } ?: return
        classDiagramEditor.deletePresentation(classPresentation)
    }

    private fun deleteLinkPresentation(receivedPoints: List<Point2D>, linkType: String) {
        logger.debug("Delete link presentation.")
        val diagram = diagramViewManager.currentDiagram
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