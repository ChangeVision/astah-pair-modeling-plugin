/*
 * ProjectSyncReceiver.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.*
import com.change_vision.jude.api.inf.presentation.ILinkPresentation
import com.change_vision.jude.api.inf.presentation.INodePresentation
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.util.*

class ProjectSyncReceiver(
    private val entityLUT: EntityLUT,
    private val mqttPublisher: MqttPublisher,
    clientId: String
) :
    Receiver(clientId) {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor

    @ExperimentalSerializationApi
    override fun messageArrived(topic: String, message: MqttMessage) {
        if (isReceivedMyself(topic)) return
        val receivedMessage = Cbor.decodeFromByteArray<String>(message.payload)
        logger.debug("Received: $receivedMessage ($topic)")
        if (receivedMessage == "projectsync") {
            sync()
        }
    }

    @ExperimentalSerializationApi
    private fun sync() {
        logger.debug("Project Sync")
        val project = projectAccessor.project

        entityLUT.entries.add(Entry(project.id, project.id))
        val createProjectTransaction = Transaction(listOf(CreateProject(project.name, project.id)))
        encodeAndPublish(createProjectTransaction)

        // TODO: 2. プロジェクトの起点から一つずつ(対応している)エンティティを送る
        // diagram
        val createDiagramOperations = project.diagrams.mapNotNull {
            when (it) {
                is IClassDiagram -> createClassDiagram(it)
                is IMindMapDiagram -> createMindMapDiagram(it)
                is IStateMachineDiagram -> createStateMachineDiagram(it)
                else -> {
                    logger.debug("${it}(Unknown diagram)")
                    null
                }
            }
        }
        if (createDiagramOperations.isNotEmpty()) {
            val createTransaction = Transaction(createDiagramOperations)
            encodeAndPublish(createTransaction)
        }

        // model
        logger.debug("$project owned entities: ${project.ownedElements.map { it.name ?: "No name" }}}")
        val createModelOperations = project.ownedElements.mapNotNull {
            when (it) {
                is IClass -> createClassModel(it)
                else -> {
                    logger.debug("${it}(Unknown)")
                    null
                }
            }
        }
        if (createModelOperations.isNotEmpty()) {
            val createTransaction = Transaction(createModelOperations)
            encodeAndPublish(createTransaction)
        }
        // attributes, operations
        project.ownedElements.filterIsInstance<IClass>().forEach {
            val createAttributeOperations = it.attributes.mapNotNull { attribute ->
                createAttribute(attribute)
            }
            if (createAttributeOperations.isNotEmpty()) {
                val createTransaction = Transaction(createAttributeOperations)
                encodeAndPublish(createTransaction)
            }
            val createOperationOperations = it.operations.mapNotNull { operation ->
                createOperation(operation)
            }
            if (createOperationOperations.isNotEmpty()) {
                val createTransaction = Transaction(createOperationOperations)
                encodeAndPublish(createTransaction)
            }
        }

        // associations, generalizations, realizations
        val associationsDuplicated =
            project.ownedElements.filterIsInstance<IClass>()
                .map { it.attributes.mapNotNull { attribute -> attribute.association } }
                .flatten()
        val associations = associationsDuplicated.sortedBy {
            it.memberEnds.map { it.owner.id }.toSet().toString()
        }
        val sortedIdSet =
            associations.joinToString { association -> "(${association.memberEnds.joinToString { it.owner.id }}) " }
        logger.debug("sorted: $sortedIdSet")
        val createAssociationOperations =
            associations.filterIndexed { index, _ -> index % 2 == 0 }.mapNotNull { association ->
                createAssociationModel(association)
            }
        if (createAssociationOperations.isNotEmpty()) {
            val createTransaction = Transaction(createAssociationOperations)
            logger.debug("association: $createTransaction")
            encodeAndPublish(createTransaction)
        }
        val createGeneralizationOperations = project.ownedElements.filterIsInstance<IClass>()
            .map { it.generalizations.mapNotNull { generalization -> createGeneralizationModel(generalization) } }
            .flatten()
        if (createGeneralizationOperations.isNotEmpty()) {
            val createTransaction = Transaction(createGeneralizationOperations)
            logger.debug("generalization: $createTransaction")
            encodeAndPublish(createTransaction)
        }
        val createRealizationOperations = project.ownedElements.filterIsInstance<IClass>()
            .map { it.clientRealizations.mapNotNull { realization -> createRealizationModel(realization) } }
            .flatten()
        if (createRealizationOperations.isNotEmpty()) {
            val createTransaction = Transaction(createRealizationOperations)
            logger.debug("realization: $createTransaction")
            encodeAndPublish(createTransaction)
        }

        // presentation
        project.diagrams.filterIsInstance<IMindMapDiagram>().forEach {
            val diagram = it
            val createMindmapDiagramFloatingTopicOperation = diagram.floatingTopics.mapNotNull {
                createFloatingTopic(it)
            }
            if (createMindmapDiagramFloatingTopicOperation.isNotEmpty()) {
                val createTransaction = Transaction(createMindmapDiagramFloatingTopicOperation)
                encodeAndPublish(createTransaction)
            }

            val rootTopics = mutableListOf(diagram.root) + diagram.floatingTopics
            val createMindmapDiagramTopicOperation = getTopics(rootTopics)
            if (createMindmapDiagramTopicOperation.isNotEmpty()) {
                val createTransaction = Transaction(createMindmapDiagramTopicOperation)
                encodeAndPublish(createTransaction)
            }
        }
        project.diagrams.filterIsInstance<IClassDiagram>().forEach { diagram ->
            val createNodePresentationOperation =
                diagram.presentations.filterIsInstance<INodePresentation>().mapNotNull {
                    when (it.model) {
                        is IClass -> createClassPresentation(it)
                        is IComment -> createNotePresentation(it)
                        else -> {
                            logger.debug("$it(Unknown INodePresentation)")
                            null
                        }
                    }
                }
            if (createNodePresentationOperation.isNotEmpty()) {
                val createTransaction = Transaction(createNodePresentationOperation)
                encodeAndPublish(createTransaction)
            }
            val createLinkPresentationOperation =
                diagram.presentations.filterIsInstance<ILinkPresentation>().mapNotNull {
                    createLinkPresentation(it)
                }
            if (createLinkPresentationOperation.isNotEmpty()) {
                val createTransaction = Transaction(createLinkPresentationOperation)
                logger.debug("link presentation: $createTransaction")
                encodeAndPublish(createTransaction)
            }
        }
        project.diagrams.filterIsInstance<IStateMachineDiagram>().forEach { diagram ->
            val createNodePresentationOperation =
                diagram.presentations.filterIsInstance<INodePresentation>().mapNotNull {
                    when (it.model) {
                        is IPseudostate -> createPseudostate(it)
                        is IFinalState -> createFinalState(it)
                        is IState -> createState(it)
                        else -> {
                            logger.debug("Unknown")
                            null
                        }
                    }
                }
            if (createNodePresentationOperation.isNotEmpty()) {
                val createTransaction = Transaction(createNodePresentationOperation)
                logger.debug("state machine node presentation: $createTransaction")
                encodeAndPublish(createTransaction)
            }
        }
        project.diagrams.filterIsInstance<IStateMachineDiagram>().forEach { diagram ->
            val createLinkPresentationOperation =
                diagram.presentations.filterIsInstance<ILinkPresentation>().mapNotNull {
                    when (it.model) {
                        is ITransition -> createTransition(it)
                        else -> {
                            logger.debug("Unknown")
                            null
                        }
                    }
                }
            if (createLinkPresentationOperation.isNotEmpty()) {
                val createTransaction = Transaction(createLinkPresentationOperation)
                logger.debug("state machine link presentation: $createTransaction")
                encodeAndPublish(createTransaction)
            }
        }
    }

    private fun getTopics(rootTopics: List<INodePresentation>): List<CreateTopic> {
        // add children topics of root / floating topics to ArrayDeque
        val queue = ArrayDeque<INodePresentation>()
        rootTopics.filterNot { it.children == null }
            .forEach { parentTopic -> parentTopic.children.forEach { queue.add(it) } }

        val operations = mutableListOf<CreateTopic>()
        // get children topics by breadth first search
        while (true) {
            val topic = queue.poll() ?: break
            operations.add(createTopic(topic))
            topic.children.filterNotNull().forEach { queue.add(it) }
        }

        return operations.toList()
    }

    private fun createClassDiagram(entity: IClassDiagram): ClassDiagramOperation {
        val owner = entity.owner as INamedElement
        // TODO: ownerのIDをどうにかして共有させる
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val createClassDiagram = CreateClassDiagram(entity.name, owner.name, lut.common)
        logger.debug("${entity.name}(IClassDiagram)")
        return createClassDiagram
    }

    private fun createMindMapDiagram(entity: IMindMapDiagram): CreateMindmapDiagram {
        val owner = entity.owner as INamedElement
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val rootTopic = entity.root
        val rootTopicLut = entityLUT.entries.find { it.mine == rootTopic.id } ?: run {
            logger.debug("${rootTopic.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${entity.name}(IMindMapDiagram)")
        return CreateMindmapDiagram(entity.name, owner.name, rootTopicLut.common, lut.common)
    }

    private fun createStateMachineDiagram(entity: IStateMachineDiagram): CreateStateMachineDiagram {
        val owner = entity.owner as INamedElement
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val createStateMachineDiagram = CreateStateMachineDiagram(entity.name, owner.name, lut.common)
        logger.debug("$entity(IStateMachineDiagram)")
        return createStateMachineDiagram
    }

    private fun createClassModel(entity: IClass): CreateClassModel {
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
//        val parentEntry = entityLUT.entries.find { it.mine == entity.owner.id } ?: run {
//            logger.debug("${entity.owner.id} not found on LUT.")
//            return null
//        }
        // TODO: ownerのIDをLUTに登録できるようにする(最初はパッケージのIDを登録していないので取れない)
        val owner = entity.owner as INamedElement
        val createClassModel = CreateClassModel(entity.name, owner.name, entity.stereotypes.toList(), lut.common)
        logger.debug("${entity.name}(IClass)")
        return createClassModel
    }

    private fun createOperation(entity: IOperation): CreateOperation {
        val owner = entity.owner
        val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
            logger.debug("${owner.id} not found on LUT.")
            val entry = Entry(owner.id, owner.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${entity.name}(IOperation) - $owner(IClass)")
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        return CreateOperation(ownerEntry.common, entity.name, entity.returnTypeExpression, lut.common)
    }

    private fun createAttribute(entity: IAttribute): CreateAttribute {
        val owner = entity.owner
        val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
            logger.debug("${owner.id} not found on LUT.")
            val entry = Entry(owner.id, owner.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${entity.name}(IAttribute) - ${owner}(IClass)")
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        return CreateAttribute(ownerEntry.common, entity.name, entity.typeExpression, lut.common)
    }

    private fun createAssociationModel(entity: IAssociation): CreateAssociationModel? {
        val sourceClass = entity.memberEnds.first().owner
        val destinationClass = entity.memberEnds.last().owner
        return when (sourceClass) {
            is IClass -> {
                when (destinationClass) {
                    is IClass -> {
                        val sourceClassEntry = entityLUT.entries.find { it.mine == sourceClass.id } ?: run {
                            logger.debug("${sourceClass.id} not found on LUT.")
                            val entry = Entry(sourceClass.id, sourceClass.id)
                            entityLUT.entries.add(entry)
                            entry
                        }
                        val destinationClassEntry = entityLUT.entries.find { it.mine == destinationClass.id } ?: run {
                            logger.debug("${destinationClass.id} not found on LUT.")
                            val entry = Entry(destinationClass.id, destinationClass.id)
                            entityLUT.entries.add(entry)
                            entry
                        }
                        val sourceClassNavigability = entity.memberEnds.first().navigability
                        val destinationClassNavigability = entity.memberEnds.last().navigability
                        logger.debug("${sourceClass.name}(IClass, $sourceClassNavigability) - ${entity.name}(IAssociation) - ${destinationClass.name}(IClass, $destinationClassNavigability)")
                        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
                            logger.debug("${entity.id} not found on LUT.")
                            val entry = Entry(entity.id, entity.id)
                            entityLUT.entries.add(entry)
                            entry
                        }
                        CreateAssociationModel(
                            sourceClassEntry.common,
                            sourceClassNavigability,
                            destinationClassEntry.common,
                            destinationClassNavigability,
                            entity.name,
                            lut.common,
                        )
                    }
                    else -> {
                        logger.debug("${sourceClass.name}(IClass) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                        null
                    }
                }
            }
            else -> {
                logger.debug("$sourceClass(Unknown) - ${entity.name}(IAssociation) - $destinationClass(Unknown)")
                null
            }
        }
    }

    private fun createGeneralizationModel(entity: IGeneralization): ClassDiagramOperation {
        val superClass = entity.superType
        val subClass = entity.subType
        val superClassEntry = entityLUT.entries.find { it.mine == superClass.id } ?: run {
            logger.debug("${superClass.id} not found on LUT.")
            val entry = Entry(superClass.id, superClass.id)
            entityLUT.entries.add(entry)
            entry
        }
        val subClassEntry = entityLUT.entries.find { it.mine == subClass.id } ?: run {
            logger.debug("${subClass.id} not found on LUT.")
            val entry = Entry(subClass.id, subClass.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${superClass.name}(IClass) -> ${entity.name}(IGeneralization) - ${subClass.name}(IClass)")
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        return CreateGeneralizationModel(superClassEntry.common, subClassEntry.common, entity.name, lut.common)
    }

    private fun createRealizationModel(entity: IRealization): ClassDiagramOperation {
        val supplierClass = entity.supplier
        val clientClass = entity.client
        val supplierClassEntry = entityLUT.entries.find { it.mine == supplierClass.id } ?: run {
            logger.debug("${supplierClass.id} not found on LUT.")
            val entry = Entry(supplierClass.id, supplierClass.id)
            entityLUT.entries.add(entry)
            entry
        }
        val clientClassEntry = entityLUT.entries.find { it.mine == clientClass.id } ?: run {
            logger.debug("${clientClass.id} not found on LUT.")
            val entry = Entry(clientClass.id, clientClass.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${supplierClass.name}(IClass) -> ${entity.name}(IRealization) -> ${clientClass.name}(IClass)")
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        return CreateRealizationModel(supplierClassEntry.common, clientClassEntry.common, entity.name, lut.common)
    }

    private fun createFloatingTopic(entity: INodePresentation): CreateFloatingTopic {
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${entity.label}(INodePresentation, FloatingTopic)")
        return CreateFloatingTopic(entity.label, location, size, entity.diagram.name, lut.common)
    }

    private fun createTopic(entity: INodePresentation): CreateTopic {
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val parentEntry = entityLUT.entries.find { it.mine == entity.parent.id } ?: run {
            logger.debug("${entity.parent.id} not found on LUT.")
            val entry = Entry(entity.parent.id, entity.parent.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("${entity.parent.label}(INodePresentation) - ${entity.label}(INodePresentation)")
        return CreateTopic(parentEntry.common, entity.label, entity.diagram.name, lut.common)
    }

    private fun createClassPresentation(entity: INodePresentation): CreateClassPresentation {
        val model = entity.model as IClass
        val classModelEntry = entityLUT.entries.find { it.mine == model.id } ?: run {
            logger.debug("${model.id} not found on LUT.")
            val entry = Entry(model.id, model.id)
            entityLUT.entries.add(entry)
            entry
        }
        val location = Pair(entity.location.x, entity.location.y)
        logger.debug(
            "${entity.label}(INodePresentation)::${model.name}(IClass, ${
                Pair(
                    entity.width,
                    entity.height
                )
            } at ${entity.location})"
        )
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        return CreateClassPresentation(classModelEntry.common, location, diagramEntry.common, lut.common)
    }

    fun createNotePresentation(entity: INodePresentation): CreateNote {
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        logger.debug("${entity.label}(INodePresentation) - ${entity.model}(IComment)")

        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        return CreateNote(entity.label, location, size, diagramEntry.common, lut.common)
    }

    private fun createLinkPresentation(entity: ILinkPresentation): CreateLinkPresentation? {
        val source = entity.source.model
        val target = entity.target.model
        logger.debug("Model: ${entity.model::class.java}")
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        return when (source) {
            is IClass -> {
                when (target) {
                    is IClass -> {
                        val modelEntry = entityLUT.entries.find { it.mine == entity.model.id } ?: run {
                            logger.debug("${entity.model.id} not found on LUT.")
                            val entry = Entry(entity.model.id, entity.model.id)
                            entityLUT.entries.add(entry)
                            entry
                        }
                        val sourceEntry = entityLUT.entries.find { it.mine == source.id } ?: run {
                            logger.debug("${source.id} not found on LUT.")
                            val entry = Entry(source.id, source.id)
                            entityLUT.entries.add(entry)
                            entry
                        }
                        val targetEntry = entityLUT.entries.find { it.mine == target.id } ?: run {
                            logger.debug("${target.id} not found on LUT.")
                            val entry = Entry(target.id, target.id)
                            entityLUT.entries.add(entry)
                            entry
                        }
                        when (entity.model) {
                            is IAssociation -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IAssociation) - ${target.name}(IClass)")
                                CreateLinkPresentation(
                                    modelEntry.common,
                                    sourceEntry.common,
                                    targetEntry.common,
                                    LinkType.Association,
                                    diagramEntry.common,
                                    lut.common
                                )
                            }
                            is IGeneralization -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::IGeneralization) - ${target.name}(IClass)")
                                entityLUT.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    modelEntry.common,
                                    sourceEntry.common,
                                    targetEntry.common,
                                    LinkType.Generalization,
                                    diagramEntry.common,
                                    lut.common
                                )
                            }
                            is IRealization -> {
                                logger.debug("${source.name}(IClass, interface) - ${entity.label}(ILinkPresentation::IRealization) - ${target.name}(IClass)")
                                entityLUT.entries.add(Entry(entity.id, entity.id))
                                CreateLinkPresentation(
                                    modelEntry.common,
                                    sourceEntry.common,
                                    targetEntry.common,
                                    LinkType.Realization,
                                    diagramEntry.common,
                                    lut.common
                                )
                            }
                            else -> {
                                logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation::Unknown) - ${target.name}(IClass)")
                                null
                            }
                        }
                    }
                    else -> {
                        logger.debug("${source.name}(IClass) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                        null
                    }
                }
            }
            else -> {
                logger.debug("$source(Unknown) - ${entity.label}(ILinkPresentation) - $target(Unknown)")
                null
            }
        }
    }

    private fun createPseudostate(entity: INodePresentation): CreatePseudostate {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IPseudostate) not found on LUT.")
                    val entry = Entry(entity.parent.model.id, entity.parent.model.id)
                    entityLUT.entries.add(entry)
                    entry
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("$entity(INodePresentation, IPseudostate)")
        return CreatePseudostate(lut.common, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun createFinalState(entity: INodePresentation): CreateFinalState {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.id }
                ?: run {
                    logger.debug("${entity.parent.model.id}(Parent of INodePresentation, IFinalState) not found on LUT.")
                    val entry = Entry(entity.parent.model.id, entity.parent.model.id)
                    entityLUT.entries.add(entry)
                    entry
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("$entity(INodePresentation, IFinalState)")
        return CreateFinalState(lut.common, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun createState(entity: INodePresentation): CreateState {
        val parentEntry =
            if (entity.parent == null) Entry("", "") else entityLUT.entries.find { it.mine == entity.parent.model.id }
                ?: run {
                    logger.debug("${entity.model.id}(Parent of INodePresentation, IState) not found on LUT.")
                    val entry = Entry(entity.parent.model.id, entity.parent.model.id)
                    entityLUT.entries.add(entry)
                    entry
                }
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("$entity(INodePresentation, IState)")
        return CreateState(lut.common, entity.label, location, size, parentEntry.common, diagramEntry.common)
    }

    private fun createTransition(entity: ILinkPresentation): CreateTransition {
        val sourceEntry = entityLUT.entries.find { it.mine == entity.source.model.id } ?: run {
            logger.debug("${entity.source.model.id}(Source of INodePresentation, IState) not found on LUT.")
            val entry = Entry(entity.source.model.id, entity.source.model.id)
            entityLUT.entries.add(entry)
            entry
        }

        val targetEntry = entityLUT.entries.find { it.mine == entity.target.model.id } ?: run {
            logger.debug("${entity.model.id}(INodePresentation, IState) not found on LUT.")
            val entry = Entry(entity.model.id, entity.model.id)
            entityLUT.entries.add(entry)
            entry
        }

        val lut = entityLUT.entries.find { it.mine == entity.id } ?: run {
            logger.debug("${entity.id} not found on LUT.")
            val entry = Entry(entity.id, entity.id)
            entityLUT.entries.add(entry)
            entry
        }
        val diagramEntry = entityLUT.entries.find { it.mine == entity.diagram.id } ?: run {
            logger.debug("${entity.diagram.id} not found on LUT.")
            val entry = Entry(entity.diagram.id, entity.diagram.id)
            entityLUT.entries.add(entry)
            entry
        }
        logger.debug("$entity(ILinkPresentation, ITransition)")
        return CreateTransition(lut.common, entity.label, sourceEntry.common, targetEntry.common, diagramEntry.common)
    }

    @ExperimentalSerializationApi
    private fun encodeAndPublish(transaction: Transaction) {
        val byteArray = Cbor.encodeToByteArray(transaction)
        mqttPublisher.publish(byteArray)
    }

    companion object : Logging {
        private val logger = logger()
    }
}