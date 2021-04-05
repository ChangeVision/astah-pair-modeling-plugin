/*
 * MqttSubscriber.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttSubscriber(
    brokerAddress: String,
    private val topic: String,
    private val clientId: String,
    private val receiver: IReceiver
) : MqttCallback {
    private var broker: String = "tcp://$brokerAddress:1883"
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

    override fun messageArrived(topic: String, message: MqttMessage) {
        // If the message was send by myself,  ignore this one.
        val senderClientId = topic.split("/").last()
        if (senderClientId == clientId) {
            return
        }
        logger.debug("Received message: $topic")
        receiver.receive(senderClientId, message)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        TODO("Not yet implemented")
    }

    companion object : Logging {
        private val logger = logger()
    }

}