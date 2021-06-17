/*
 * MqttSubscriber.kt - pair-modeling
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
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttSubscriber(brokerAddress: String, brokerPortNumber: Int, clientId: String) {
    private var mqttClient: MqttClient

    init {
        val broker = "tcp://$brokerAddress:$brokerPortNumber"
        val clientIdSubscriber = "$clientId/sub"
        mqttClient = MqttClient(broker, clientIdSubscriber, MemoryPersistence())
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.connectionTimeout = 10
        mqttClient.connect(mqttConnectOptions)
        logger.info("Connected to broker $broker")
    }

    fun subscribe(topic: String, receiver: Receiver) {
        val qos = 2
        mqttClient.subscribe(topic, qos, receiver)
        logger.info("Subscribed $topic")
    }

    fun close() {
        mqttClient.disconnect()
        mqttClient.close()
        logger.info("Closed connection.")
    }

    companion object : Logging {
        private val logger = logger()
    }

}