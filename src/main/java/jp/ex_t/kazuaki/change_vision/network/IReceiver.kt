/*
 * IReceiver.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import org.eclipse.paho.client.mqttv3.MqttMessage

interface IReceiver {
    fun receive(senderClientId: String, message: MqttMessage)
}