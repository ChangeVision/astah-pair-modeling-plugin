/*
 * MqttPublisher.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttPublisher(brokerAddress: String, brokerPortNumber: Int, private val topic: String, clientId: String) {
    private val qos = 2
    private var broker: String = "tcp://$brokerAddress:$brokerPortNumber"
    private val clientIdPublisher = "$clientId/pub"

    @Throws(MqttException::class)
    fun publish(message: ByteArray) {
        try {
            val mqttClient = MqttClient(broker, clientIdPublisher, MemoryPersistence())
            val mqttConnectOptions = MqttConnectOptions()
            mqttConnectOptions.isCleanSession = false
            mqttConnectOptions.connectionTimeout = 10

            mqttClient.connect(mqttConnectOptions)
            logger.info("Connected to broker $broker.")
            val mqttMessage = MqttMessage(message)
            mqttMessage.qos = qos
            mqttClient.publish(topic, mqttMessage)
            mqttClient.disconnect()
            mqttClient.close()
            logger.info("Closed connection.")
        } catch (e: MqttException) {
            logger.error("Publisher error.", e)
            throw e
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}