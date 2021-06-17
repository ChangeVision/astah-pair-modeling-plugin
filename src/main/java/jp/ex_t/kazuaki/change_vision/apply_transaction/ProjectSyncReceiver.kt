/*
 * ProjectSyncReceiver.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.MqttPublisher
import jp.ex_t.kazuaki.change_vision.network.Receiver
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.MqttMessage

class ProjectSyncReceiver(mqttPublisher: MqttPublisher, clientId: String) : Receiver(clientId) {
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

    private fun sync() {
        // TODO: 1. プロジェクトを新規作成させる
        // TODO: 2. プロジェクトの起点から一つずつ(対応している)エンティティを送る
        logger.debug("Project Sync")
    }

    companion object : Logging {
        private val logger = logger()
    }
}