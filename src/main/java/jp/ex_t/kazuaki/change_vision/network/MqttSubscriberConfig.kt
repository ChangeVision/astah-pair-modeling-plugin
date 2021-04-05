/*
 * MqttSubscribeConfig.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

data class MqttSubscriberConfig(
    val topic: String,
    val qos: Int,
    val receiver: IReceiver
)
