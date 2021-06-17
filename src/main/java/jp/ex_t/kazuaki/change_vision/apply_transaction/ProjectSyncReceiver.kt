/*
 * ProjectSyncReceiver.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.model.IClassDiagram
import com.change_vision.jude.api.inf.model.IMindMapDiagram
import com.change_vision.jude.api.inf.model.INamedElement
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.eclipse.paho.client.mqttv3.MqttMessage

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
        entityLUT.entries.add(Entry(projectAccessor.project.id, projectAccessor.project.id))
        val createProjectTransaction = Transaction(listOf(CreateProject(projectAccessor.project.id)))
        encodeAndPublish(createProjectTransaction)

        // TODO: 2. プロジェクトの起点から一つずつ(対応している)エンティティを送る
        // 図要素
        val createDiagramOperations = projectAccessor.project.diagrams.mapNotNull {
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

        val rootModel = projectAccessor.project
        logger.debug("$rootModel owned entities: ${rootModel.ownedElements.map { it.name ?: "No name" }}}")
        val createOperations = rootModel.ownedElements.mapNotNull {
            when (it) {
                is IClass -> createClassModel(it)
                else -> {
                    logger.debug("${it}(Unknown)")
                    null
                }
            }
        }
        if (createOperations.isNotEmpty()) {
            val createTransaction = Transaction(createOperations)
            encodeAndPublish(createTransaction)
        }
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

    @ExperimentalSerializationApi
    private fun encodeAndPublish(transaction: Transaction) {
        val byteArray = Cbor.encodeToByteArray(transaction)
        mqttPublisher.publish(byteArray)
    }

    companion object : Logging {
        private val logger = logger()
    }
}