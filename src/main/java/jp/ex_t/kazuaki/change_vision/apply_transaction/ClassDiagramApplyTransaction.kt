/*
 * ClassDiagramApplyTransaction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.INodePresentation
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import java.awt.geom.Point2D

class ClassDiagramApplyTransaction(private val entityLUT: EntityLUT) : IApplyTransaction<ClassDiagramOperation> {
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
                is CreateNote -> {
                    validateAndCreateNotePresentation(it)
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
                is ResizeNote -> {
                    validateAndResizeNote(it)
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
                is DeleteModel -> {
                    validateAndDeleteModel(it)
                }
                is DeletePresentation -> {
                    validateAndDeletePresentation(it)
                }
                is DeleteNote -> {
                    validateAndDeleteNote(it)
                }
            }
        }
    }

    private fun validateAndCreateClassDiagram(operation: CreateClassDiagram) {
        if (operation.name.isNotEmpty() && operation.ownerName.isNotEmpty() && operation.id.isNotEmpty()) {
            createClassDiagram(operation.name, operation.ownerName, operation.id)
        }
    }

    private fun validateAndCreateClassModel(operation: CreateClassModel) {
        if (operation.name.isNotEmpty() && operation.parentName.isNotEmpty() && operation.id.isNotEmpty()) {
            createClassModel(operation.name, operation.parentName, operation.stereotypes, operation.id)
        }
    }

    private fun validateAndCreateAssociationModel(operation: CreateAssociationModel) {
        if (operation.sourceClassId.isNotEmpty()
            && operation.sourceClassNavigability.isNotEmpty()
            && operation.destinationClassId.isNotEmpty()
            && operation.destinationClassNavigability.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createAssociationModel(
                operation.sourceClassId,
                operation.sourceClassNavigability,
                operation.destinationClassId,
                operation.destinationClassNavigability,
                operation.name,
                operation.id,
            )
        }
    }

    private fun validateAndCreateGeneralizationModel(operation: CreateGeneralizationModel) {
        if (operation.superClassId.isNotEmpty()
            && operation.subClassId.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createGeneralizationModel(
                operation.superClassId,
                operation.subClassId,
                operation.name,
                operation.id,
            )
        }
    }

    private fun validateAndCreateRealizationModel(operation: CreateRealizationModel) {
        if (operation.supplierClassId.isNotEmpty()
            && operation.clientClassId.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createRealizationModel(
                operation.supplierClassId,
                operation.clientClassId,
                operation.name,
                operation.id
            )
        }
    }

    private fun validateAndCreateClassPresentation(operation: CreateClassPresentation) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.diagramName.isNotEmpty() && operation.classId.isNotEmpty() && operation.id.isNotEmpty()) {
            createClassPresentation(operation.classId, location, operation.diagramName, operation.id)
        }
    }

    private fun validateAndCreateLinkPresentation(operation: CreateLinkPresentation) {
        if (operation.sourceClassId.isNotEmpty()
            && operation.targetClassId.isNotEmpty()
            && operation.diagramName.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createLinkPresentation(
                operation.sourceClassId,
                operation.targetClassId,
                operation.linkType,
                operation.diagramName,
                operation.id,
            )
        }
    }

    private fun validateAndCreateNotePresentation(operation: CreateNote) {
        if (operation.id.isNotEmpty() && operation.diagramName.isNotEmpty()) {
            val location = Point2D.Double(operation.location.first, operation.location.second)
            createNotePresentation(operation.note, location, operation.size, operation.id, operation.diagramName)
        }
    }

    private fun validateAndCreateOperation(operation: CreateOperation) {
        if (operation.ownerId.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.returnTypeExpression.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createOperation(
                operation.ownerId,
                operation.name,
                operation.returnTypeExpression,
                operation.id,
            )
        }
    }

    private fun validateAndCreateAttribute(operation: CreateAttribute) {
        if (operation.ownerId.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.typeExpression.isNotEmpty()
            && operation.id.isNotEmpty()
        ) {
            createAttribute(operation.ownerId, operation.name, operation.typeExpression, operation.id)
        }
    }

    private fun validateAndResizeClassPresentation(
        operation: ResizeClassPresentation,
        operations: List<ClassDiagramOperation>
    ) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (!operations.any { it is ChangeClassModel }
            && operation.id.isNotEmpty()
            && operation.diagramName.isNotEmpty()) {
            resizeClassPresentation(
                operation.id,
                location,
                operation.size,
                operation.diagramName
            )
        }
    }

    private fun validateAndResizeNote(operation: ResizeNote) {
        val location = Point2D.Double(operation.location.first, operation.location.second)
        if (operation.id.isNotEmpty() && operation.diagramName.isNotEmpty()) {
            resizeNote(operation.id, operation.note, location, operation.size, operation.diagramName)
        }
    }

    private fun validateAndChangeClassModel(operation: ChangeClassModel) {
        if (operation.id.isNotEmpty() && operation.name.isNotEmpty()) {
            changeClassModel(operation.id, operation.name, operation.stereotypes)
        }
    }

    private fun validateAndChangeOperationNameAndReturnTypeExpression(operation: ChangeOperationNameAndReturnTypeExpression) {
        if (operation.ownerId.isNotEmpty()
            && operation.id.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.returnTypeExpression.isNotEmpty()
        ) {
            changeOperationNameAndReturnTypeExpression(
                operation.ownerId,
                operation.id,
                operation.name,
                operation.returnTypeExpression
            )
        }
    }

    private fun validateAndChangeAttributeNameAndTypeExpression(operation: ChangeAttributeNameAndTypeExpression) {
        if (operation.ownerId.isNotEmpty()
            && operation.id.isNotEmpty()
            && operation.name.isNotEmpty()
            && operation.typeExpression.isNotEmpty()
        ) {
            changeAttributeNameAndTypeExpression(
                operation.ownerId,
                operation.id,
                operation.name,
                operation.typeExpression
            )
        }
    }

    private fun validateAndDeleteModel(operation: DeleteModel) {
        if (operation.id.isNotEmpty()) {
            deleteModel(operation.id)
        }
    }

    private fun validateAndDeletePresentation(operation: DeletePresentation) {
        if (operation.id.isNotEmpty()) {
            deletePresentation(operation.id)
        }
    }

    private fun validateAndDeleteNote(operation: DeleteNote) {
        if (operation.id.isNotEmpty()) {
            deleteNote(operation.id)
        }
    }

    private fun createClassDiagram(name: String, ownerName: String, id: String) {
        logger.debug("Create class diagram.")
        val owner = projectAccessor.findElements(INamedElement::class.java, ownerName).first() as INamedElement
        val diagram = classDiagramEditor.createClassDiagram(owner, name)
        entityLUT.entries.add(Entry(diagram.id, id))
        diagramViewManager.open(diagram)
    }

    private fun createClassModel(name: String, parentName: String, stereotypes: List<String?>, id: String) {
        logger.debug("Create class model.")
        val parent = projectAccessor.findElements(IPackage::class.java, parentName).first() as IPackage? ?: run {
            logger.debug("Parent $parentName not found.")
            return
        }
        val classModel = basicModelEditor.createClass(parent, name)
        stereotypes.forEach {
            classModel.addStereotype(it)
        }
        entityLUT.entries.add(Entry(classModel.id, id))
//        val parentEntry = entityLUT.entries.find { it.common == parentId } ?: run {
//            logger.debug("$parentId not found on LUT.")
//            return
//        }
//        val parentPackage = projectAccessor.findElements(IPackage::class.java).find { it.id == parentEntry.mine }
//        val parentClass = projectAccessor.findElements(IClass::class.java).find { it.id == parentEntry.mine }
//        if (parentPackage != null) {
//            val classModel = basicModelEditor.createClass(parentPackage as IPackage, name)
//            stereotypes.forEach {
//                classModel.addStereotype(it)
//            }
//            entityLUT.entries.add(Entry(classModel.id, id))
//        } else if (parentClass != null) {
//            val classModel = basicModelEditor.createClass(parentClass as IClass, name)
//            stereotypes.forEach {
//                classModel.addStereotype(it)
//            }
//            entityLUT.entries.add(Entry(classModel.id, id))
//        } else {
//            logger.debug("$parentId not found on LUT.")
//            return
//        }
    }

    private fun createAssociationModel(
        sourceClassId: String,
        sourceClassNavigability: String,
        destinationClassId: String,
        destinationClassNavigability: String,
        associationName: String,
        id: String
    ) {
        logger.debug("Create association model.")
        val sourceClassEntry = entityLUT.entries.find { it.common == sourceClassId } ?: run {
            logger.debug("$sourceClassId not found on LUT.")
            return
        }
        val destinationClassEntry = entityLUT.entries.find { it.common == destinationClassId } ?: run {
            logger.debug("$destinationClassId not found on LUT.")
            return
        }
        val sourceClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == sourceClassEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${sourceClassEntry.mine} not found but $sourceClassId found on LUT.")
                return
            }
        val destinationClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == destinationClassEntry.mine } as IClass?
                ?: run {
                    logger.debug("IClass ${destinationClassEntry.mine} not found but $destinationClassId found on LUT.")
                    return
                }

        val association = basicModelEditor.createAssociation(sourceClass, destinationClass, associationName, "", "")
        entityLUT.entries.add(Entry(association.id, id))
        sourceClass.attributes.filterNot { it.association == null }
            .find { it.association == association }?.navigability = sourceClassNavigability
        destinationClass.attributes.filterNot { it.association == null }
            .find { it.association == association }?.navigability = destinationClassNavigability
    }

    private fun createGeneralizationModel(superClassId: String, subClassId: String, name: String, id: String) {
        logger.debug("Create generalization model.")
        val superClassEntry = entityLUT.entries.find { it.common == superClassId } ?: run {
            logger.debug("$superClassId not found on LUT.")
            return
        }
        val subClassEntry = entityLUT.entries.find { it.common == subClassId } ?: run {
            logger.debug("$subClassId not found on LUT.")
            return
        }
        val superClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == superClassEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${superClassEntry.mine} not found but $superClassId found on LUT.")
                return
            }
        val subClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == subClassEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${subClassEntry.mine} not found but $subClassId found on LUT.")
                return
            }
        val generalization = basicModelEditor.createGeneralization(subClass, superClass, name)
        entityLUT.entries.add(Entry(generalization.id, id))
    }

    private fun createRealizationModel(supplierClassId: String, clientClassId: String, name: String, id: String) {
        logger.debug("Create realization model.")
        val supplierClassEntry = entityLUT.entries.find { it.common == supplierClassId } ?: run {
            logger.debug("$supplierClassId not found on LUT.")
            return
        }
        val clientClassEntry = entityLUT.entries.find { it.common == clientClassId } ?: run {
            logger.debug("$clientClassId not found on LUT.")
            return
        }
        val supplierClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == supplierClassEntry.mine } as IClass?
                ?: run {
                    logger.debug("IClass ${supplierClassEntry.mine} not found but $supplierClassId found on LUT.")
                    return
                }
        val clientClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == clientClassEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${clientClassEntry.mine} not found but $clientClassId found on LUT.")
                return
            }
        val realization = basicModelEditor.createRealization(clientClass, supplierClass, name)
        entityLUT.entries.add(Entry(realization.id, id))
    }

    private fun createClassPresentation(classId: String, location: Point2D, diagramName: String, id: String) {
        logger.debug("Create class presentation.")
        val classEntry = entityLUT.entries.find { it.common == classId } ?: run {
            logger.debug("$classId not found on LUT.")
            return
        }
        val classModel =
            projectAccessor.findElements(IClass::class.java).find { it.id == classEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${classEntry.mine} not found but $classId found on LUT.")
                return
            }
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val classPresentation = classDiagramEditor.createNodePresentation(classModel, location)
        entityLUT.entries.add(Entry(classPresentation.id, id))
    }

    private fun createLinkPresentation(
        sourceClassId: String,
        targetClassId: String,
        linkType: LinkType,
        diagramName: String,
        id: String
    ) {
        logger.debug("Create link presentation.")
        @Throws(ClassNotFoundException::class)
        fun searchModel(linkType: LinkType, sourceClass: IClass, targetClass: IClass): IElement {
            when (linkType) {
                LinkType.Association -> {
                    return (sourceClass.attributes.filterNot { it.association == null }.find { sourceClassAttribute ->
                        targetClass.attributes.filterNot { it.association == null }.any { targetClassAttribute ->
                            sourceClassAttribute.association == targetClassAttribute.association
                        }
                    } ?: throw ClassNotFoundException()).association
                }
                LinkType.Generalization -> {
                    return sourceClass.generalizations.find {
                        it.superType == targetClass
                    } ?: throw ClassNotFoundException()
                }
                LinkType.Realization -> {
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
        val sourceClassEntry = entityLUT.entries.find { it.common == sourceClassId } ?: run {
            logger.debug("$sourceClassId not found on LUT.")
            return
        }
        val sourceClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == sourceClassEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${sourceClassEntry.mine} not found but $sourceClassId found on LUT.")
                return
            }
        val sourceClassPresentation = sourceClass.presentations.first { it.diagram == diagram } as INodePresentation
        val targetClassEntry = entityLUT.entries.find { it.common == targetClassId } ?: run {
            logger.debug("$targetClassId not found on LUT.")
            return
        }
        val targetClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == targetClassEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${targetClassEntry.mine} not found but $targetClassId found on LUT.")
                return
            }
        val targetClassPresentation = targetClass.presentations.first { it.diagram == diagram } as INodePresentation
        logger.debug("Source: ${sourceClass.attributes}, target: ${targetClass.attributes}")
        try {
            val element = searchModel(linkType, sourceClass, targetClass)
            logger.debug("Link: $element")
            val linkPresentation =
                classDiagramEditor.createLinkPresentation(element, sourceClassPresentation, targetClassPresentation)
            entityLUT.entries.add(Entry(linkPresentation.id, id))
        } catch (e: ClassNotFoundException) {
            logger.error("Link model not found.", e)
            return
        } catch (e: NotImplementedError) {
            logger.error("Not implemented.", e)
        }
    }

    private fun createNotePresentation(
        note: String,
        location: Point2D,
        size: Pair<Double, Double>,
        id: String,
        diagramName: String
    ) {
        logger.debug("Create note.")
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val entity = classDiagramEditor.createNote(note, location)
        entity.width = size.first
        entity.height = size.second
        entityLUT.entries.add(Entry(entity.id, id))
    }

    private fun createOperation(ownerId: String, name: String, returnTypeExpression: String, id: String) {
        logger.debug("Create operation.")
        val ownerEntry = entityLUT.entries.find { it.common == ownerId } ?: run {
            logger.debug("$ownerId not found on LUT.")
            return
        }
        val owner =
            projectAccessor.findElements(IClass::class.java).find { it.id == ownerEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${ownerEntry.mine} not found but $ownerId found on LUT.")
                return
            }
        val operation = basicModelEditor.createOperation(owner, name, returnTypeExpression)
        entityLUT.entries.add(Entry(operation.id, id))
    }

    private fun createAttribute(ownerId: String, name: String, typeExpression: String, id: String) {
        logger.debug("Create attribute.")
        val ownerEntry = entityLUT.entries.find { it.common == ownerId } ?: run {
            logger.debug("$ownerId not found on LUT.")
            return
        }
        val owner =
            projectAccessor.findElements(IClass::class.java).find { it.id == ownerEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${ownerEntry.mine} not found but $ownerId found on LUT.")
                return
            }
        val attribute = basicModelEditor.createAttribute(owner, name, typeExpression)
        entityLUT.entries.add(Entry(attribute.id, id))
    }

    private fun resizeClassPresentation(
        id: String,
        location: Point2D,
        size: Pair<Double, Double>,
        diagramName: String
    ) {
        logger.debug("Resize class presentation.")
        val (width, height) = size
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val classPresentation = diagram.presentations.find { it.id == entry.mine } as INodePresentation? ?: run {
            logger.debug("INodePresentation ${entry.mine} not found but $id found on LUT.")
            return
        }
        classPresentation.location = location
        classPresentation.width = width
        classPresentation.height = height
    }

    private fun resizeNote(
        id: String,
        note: String,
        location: Point2D,
        size: Pair<Double, Double>,
        diagramName: String
    ) {
        logger.debug("Resize class presentation.")
        val (width, height) = size
        val diagram = projectAccessor.findElements(IDiagram::class.java, diagramName).first() as IDiagram
        classDiagramEditor.diagram = diagram
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val notePresentation = diagram.presentations.find { it.id == entry.mine } as INodePresentation? ?: run {
            logger.debug("INodePresentation ${entry.mine} not found but $id found on LUT.")
            return
        }
        notePresentation.label = note
        notePresentation.location = location
        notePresentation.width = width
        notePresentation.height = height
    }

    private fun changeClassModel(id: String, name: String, stereotypes: List<String?>) {
        logger.debug("Change class model name and stereotypes.")
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val clazz = projectAccessor.findElements(IClass::class.java).find { it.id == entry.mine } as IClass? ?: run {
            logger.debug("IClass ${entry.mine} not found but $id found on LUT.")
            return
        }
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

    private fun changeOperationNameAndReturnTypeExpression(
        ownerId: String,
        id: String,
        name: String,
        returnTypeExpression: String
    ) {
        logger.debug("Change operation name and return type expression.")
        val ownerEntry = entityLUT.entries.find { it.common == ownerId } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val ownerClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == ownerEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${ownerEntry.mine} not found but $ownerId found on LUT.")
                return
            }
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val operation = ownerClass.operations.find { it.id == entry.mine } ?: run {
            logger.debug("IOperation ${entry.mine} not found but $id found on LUT.")
            return
        }
        operation.name = name
        operation.returnTypeExpression = returnTypeExpression
    }

    private fun changeAttributeNameAndTypeExpression(
        ownerId: String,
        id: String,
        name: String,
        typeExpression: String
    ) {
        logger.debug("Change attribute name and type expression.")
        val ownerEntry = entityLUT.entries.find { it.common == ownerId } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val ownerClass =
            projectAccessor.findElements(IClass::class.java).find { it.id == ownerEntry.mine } as IClass? ?: run {
                logger.debug("IClass ${ownerEntry.mine} not found but $ownerId found on LUT.")
                return
            }
        val entry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val attribute =
            ownerClass.attributes.find { it.id == entry.mine } ?: run {
                logger.debug("IAttribute ${entry.mine} not found but $id found on LUT.")
                return
            }
        attribute.name = name
        attribute.typeExpression = typeExpression
    }

    private fun deleteModel(id: String) {
        logger.debug("Delete model.")
        val diagram = diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val lutEntry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val model = projectAccessor.findElements(IEntity::class.java).find { it.id == lutEntry.mine } ?: run {
            logger.debug("Model ${lutEntry.mine} not found but $id found on LUT.")
            entityLUT.entries.remove(lutEntry)
            return
        }
        entityLUT.entries.remove(lutEntry)
        basicModelEditor.delete(model)
    }

    private fun deletePresentation(id: String) {
        logger.debug("Delete presentation.")
        val diagram = diagramViewManager.currentDiagram
        classDiagramEditor.diagram = diagram
        val lutEntry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val classPresentation = diagram.presentations.find { it.id == lutEntry.mine } ?: run {
            logger.debug("Presentation ${lutEntry.mine} not found but $id found on LUT.")
            entityLUT.entries.remove(lutEntry)
            return
        }
        entityLUT.entries.remove(lutEntry)
        classDiagramEditor.deletePresentation(classPresentation)
    }

    private fun deleteNote(id: String) {
        logger.debug("Delete note.")
        val diagram = diagramViewManager.currentDiagram
        val lutEntry = entityLUT.entries.find { it.common == id } ?: run {
            logger.debug("$id not found on LUT.")
            return
        }
        val notePresentation = diagram.presentations.find { it.id == lutEntry.mine } ?: run {
            logger.debug("Note presentation ${lutEntry.mine} not found but $id found on LUT.")
            entityLUT.entries.remove(lutEntry)
            return
        }
        entityLUT.entries.remove(lutEntry)
        basicModelEditor.delete(notePresentation.model)
    }

    companion object : Logging {
        private val logger = logger()
    }
}