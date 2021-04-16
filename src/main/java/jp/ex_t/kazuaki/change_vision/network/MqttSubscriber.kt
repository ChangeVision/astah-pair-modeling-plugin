/*
 * MqttSubscriber.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.apply_transaction.ReflectTransaction
import jp.ex_t.kazuaki.change_vision.logger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttSubscriber(
    brokerAddress: String,
    brokerPortNumber: Int,
    private val topic: String,
    private val clientId: String,
    private val reflectTransaction: ReflectTransaction
) : MqttCallback {
    private var broker: String = "tcp://$brokerAddress:$brokerPortNumber"
    private lateinit var mqttClient: MqttClient
    private val clientIdSubscriber = "$clientId/sub"

    fun subscribe() {
        val qos = 2

        mqttClient = MqttClient(broker, clientIdSubscriber, MemoryPersistence())
        mqttClient.setCallback(this)
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = false

        mqttClient.connect(mqttConnectOptions)
        logger.info("Connected to broker $broker")

        mqttClient.subscribe(topic, qos)
    }

    fun close() {
        mqttClient.disconnect()
        mqttClient.close()
        logger.info("Closed connection.")
    }

    override fun connectionLost(cause: Throwable) {
        logger.error("Connection lost.", cause)
        throw cause
        // TODO: retry?
    }

    @ExperimentalSerializationApi
    override fun messageArrived(topic: String, message: MqttMessage) {
        // If the message was send by myself,  ignore this one.
        val receivedClientId = topic.split("/").last()
        if (receivedClientId == clientId) {
            return
        }
        val receivedMessage = Cbor.decodeFromByteArray<Transaction>(message.payload)
        logger.debug("Received: $receivedMessage ($topic)")
        reflectTransaction.transact(receivedMessage)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        TODO("Not yet implemented")
    }

    companion object : Logging {
        private val logger = logger()
    }

}