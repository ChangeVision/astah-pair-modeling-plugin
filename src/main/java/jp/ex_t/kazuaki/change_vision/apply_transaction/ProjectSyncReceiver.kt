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
        }

        // presentation
        project.diagrams.filter { it is IMindMapDiagram }.forEach {
            val diagram = it as IMindMapDiagram
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
        project.diagrams.filter { it is IClassDiagram }.forEach { diagram ->
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
            operations.add(createTopic(topic) ?: continue)
            topic.children.filterNotNull().forEach { queue.add(it) }
        }

        return operations.toList()
    }

    private fun createClassDiagram(entity: IClassDiagram): ClassDiagramOperation {
        val owner = entity.owner as INamedElement
        // TODO: ownerのIDをどうにかして共有させる
        val createClassDiagram = CreateClassDiagram(entity.name, owner.name, entity.id)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("${entity.name}(IClassDiagram)")
        return createClassDiagram
    }

    private fun createMindMapDiagram(entity: IMindMapDiagram): CreateMindmapDiagram {
        val owner = entity.owner as INamedElement
        val rootTopic = entity.root
        logger.debug("${entity.name}(IMindMapDiagram)")
        entityLUT.entries.add(Entry(rootTopic.id, rootTopic.id))
        return CreateMindmapDiagram(entity.name, owner.name, rootTopic.id)
    }

    private fun createClassModel(entity: IClass): CreateClassModel {
        entityLUT.entries.add(Entry(entity.id, entity.id))
//        val parentEntry = entityLUT.entries.find { it.mine == entity.owner.id } ?: run {
//            logger.debug("${entity.owner.id} not found on LUT.")
//            return null
//        }
        // TODO: ownerのIDをLUTに登録できるようにする(最初はパッケージのIDを登録していないので取れない)
        val owner = entity.owner as INamedElement
        val createClassModel = CreateClassModel(entity.name, owner.name, entity.stereotypes.toList(), entity.id)
        logger.debug("${entity.name}(IClass)")
        return createClassModel
    }

    private fun createAttribute(entity: IAttribute): CreateAttribute? {
        val owner = entity.owner
        val ownerEntry = entityLUT.entries.find { it.mine == owner.id } ?: run {
            logger.debug("${owner.id} not found on LUT.")
            return null
        }
        logger.debug("${entity.name}(IAttribute) - ${owner}(IClass)")
        entityLUT.entries.add(Entry(entity.id, entity.id))
        return CreateAttribute(ownerEntry.common, entity.name, entity.typeExpression, entity.id)
    }

    private fun createFloatingTopic(entity: INodePresentation): CreateFloatingTopic {
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("${entity.label}(INodePresentation, FloatingTopic)")
        return CreateFloatingTopic(entity.label, location, size, entity.diagram.name, entity.id)
    }

    private fun createTopic(entity: INodePresentation): CreateTopic? {
        entityLUT.entries.add(Entry(entity.id, entity.id))
        val parentEntry = entityLUT.entries.find { it.mine == entity.parent.id } ?: run {
            logger.debug("${entity.parent.id} not found on LUT.")
            return null
        }
        logger.debug("${entity.parent.label}(INodePresentation) - ${entity.label}(INodePresentation)")
        return CreateTopic(parentEntry.common, entity.label, entity.diagram.name, entity.id)
    }

    private fun createClassPresentation(entity: INodePresentation): CreateClassPresentation? {
        val model = entity.model as IClass
        val classModelEntry = entityLUT.entries.find { it.mine == model.id } ?: run {
            logger.debug("${model.id} not found on LUT.")
            return null
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
        entityLUT.entries.add(Entry(entity.id, entity.id))
        return CreateClassPresentation(classModelEntry.common, location, entity.diagram.name, entity.id)
    }

    fun createNotePresentation(entity: INodePresentation): CreateNote {
        val location = Pair(entity.location.x, entity.location.y)
        val size = Pair(entity.width, entity.height)
        logger.debug("${entity.label}(INodePresentation) - ${entity.model}(IComment)")
        entityLUT.entries.add(Entry(entity.id, entity.id))
        return CreateNote(entity.label, location, size, entity.diagram.name, entity.id)
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