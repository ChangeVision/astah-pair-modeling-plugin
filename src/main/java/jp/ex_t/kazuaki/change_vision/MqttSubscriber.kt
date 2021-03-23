/*
 * MqttSubscriber.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttSubscriber(private val brokerAddress: String, private val topic: String, private val clientId: String, private val reflectTransaction: ReflectTransaction): MqttCallback {
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
        println("Connected to broker $broker")

        mqttClient.subscribe(topic, qos)
    }

    fun close() {
        mqttClient.disconnect()
        mqttClient.close()
        println("Closed connection.")
    }

    override fun connectionLost(cause: Throwable) {
        println("Connection lost.")
        println(cause)
        throw cause
        // TODO: retry?
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        // If the message was send by myself,  ignore this one.
        val receivedClientId = topic.split("/").last()
        if (receivedClientId == clientId) return
        val receivedMessage = Cbor.decodeFromByteArray<CreateIClass>(message.payload)
        println("Received: $receivedMessage ($topic)")
        val parentName = receivedMessage.parentName
        val childName = receivedMessage.name
        if (parentName == "" || childName == "") return
        reflectTransaction.add(parentName, childName)
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        TODO("Not yet implemented")
    }
}