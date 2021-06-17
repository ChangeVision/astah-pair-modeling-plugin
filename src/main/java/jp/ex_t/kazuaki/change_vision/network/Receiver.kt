/*
 * Receiver.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import org.eclipse.paho.client.mqttv3.IMqttMessageListener

abstract class Receiver(private val clientId: String) : IMqttMessageListener {
    fun isReceivedMyself(topic: String): Boolean {
        // If the message was send by myself,  ignore this one
        val receivedClientId = topic.split("/").last()
        if (receivedClientId == clientId) {
            return true
        }
        return false
    }
}