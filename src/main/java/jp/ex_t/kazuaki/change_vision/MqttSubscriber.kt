/*
 * MqttSubscriber.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.awt.geom.Point2D

class MqttSubscriber(brokerAddress: String, private val topic: String, private val clientId: String, private val reflectTransaction: ReflectTransaction): MqttCallback {
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
        println(cause.message)
        throw cause
        // TODO: retry?
    }

    @ExperimentalSerializationApi
    override fun messageArrived(topic: String, message: MqttMessage) {
        // If the message was send by myself,  ignore this one.
        val receivedClientId = topic.split("/").last()
        if (receivedClientId == clientId) return
        val receivedMessage = Cbor.decodeFromByteArray<Transaction>(message.payload)
        println("Received: $receivedMessage ($topic)")
        when {
            receivedMessage.createCreateClassModel != null -> {
                val createClassModel = receivedMessage.createCreateClassModel
                val name = createClassModel.name
                val parentPackageName = createClassModel.parentPackageName
                if (parentPackageName.isEmpty() || name.isEmpty()) return
                reflectTransaction.createClassModel(name, parentPackageName)
            }
            receivedMessage.createClassPresentation != null -> {
                val createClassPresentation = receivedMessage.createClassPresentation
                val className = createClassPresentation.className
                val locationPair = createClassPresentation.location
                val location = Point2D.Double(locationPair.first, locationPair.second)
                val diagramName = createClassPresentation.diagramName
                if (diagramName.isEmpty() || className.isEmpty()) return
                reflectTransaction.createClassPresentation(className, location, diagramName)
            }
            receivedMessage.resizeClassPresentation != null -> {
                val resizeClassPresentation = receivedMessage.resizeClassPresentation
                val className = resizeClassPresentation.className
                val locationPair = resizeClassPresentation.location
                val location = Point2D.Double(locationPair.first, locationPair.second)
                val size = resizeClassPresentation.size
                val diagramName = resizeClassPresentation.diagramName
                if (className.isEmpty() || diagramName.isEmpty()) return
                reflectTransaction.resizeClassPresentation(className, location, size, diagramName)
            }
            receivedMessage.createCreateAssociationModel != null -> {
                val createAssociationModel = receivedMessage.createCreateAssociationModel
                val sourceClassName = createAssociationModel.sourceClassName
                val destinationClassName = createAssociationModel.destinationClassName
                val name = createAssociationModel.name
                if (sourceClassName.isEmpty() || destinationClassName.isEmpty()) return
                reflectTransaction.createAssociationModel(sourceClassName, destinationClassName, name)
            }
            receivedMessage.createAssociationPresentation != null -> {
                val createAssociationPresentation = receivedMessage.createAssociationPresentation
                val sourceClassName = createAssociationPresentation.sourceClassName
                val targetClassName = createAssociationPresentation.targetClassName
                val diagramName = createAssociationPresentation.diagramName
                if (sourceClassName.isEmpty() || targetClassName.isEmpty() || diagramName.isEmpty()) return
                reflectTransaction.createAssociationPresentation(sourceClassName, targetClassName, diagramName)
            }
        }
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        TODO("Not yet implemented")
    }
}