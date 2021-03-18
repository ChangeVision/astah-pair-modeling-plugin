/*
 * PairModeling.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI

class PairModeling(private val topic: String, private val clientId: String, private val brokerAddress: String) {
    private val topicTransaction = "$topic/transaction"
    // TODO: もしプロジェクト全体が欲しいとなった場合はトピックを別で生やす
    // TODO: もしチャットが欲しいとなった場合はトピックを別で生やす
    private lateinit var mqttPublisher: MqttPublisher
    private lateinit var projectChangedListener: ProjectChangedListener
    private lateinit var mqttSubscriber: MqttSubscriber
    private lateinit var reflectTransaction: ReflectTransaction

    fun start() {
        val api = AstahAPI.getAstahAPI()
        val projectAccessor = api.projectAccessor

        println("Launching publisher...")
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        mqttPublisher = MqttPublisher(brokerAddress, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        println("Published: $brokerAddress:$topicTransaction ($clientId")
        println("Launched publisher.")

        println("Launching subscriber...")
        // TODO: トランザクションが相手のところで跳ね返って(イベントが掴まれる)くる問題を解消する
        val topicTransactionSubscriber = "$topicTransaction/#"
        reflectTransaction = ReflectTransaction()
        mqttSubscriber = MqttSubscriber(brokerAddress, topicTransactionSubscriber, clientId, reflectTransaction)
        mqttSubscriber.subscribe()
        println("Subscribed: $brokerAddress:$topicTransaction ($clientId")
        println("Launched subscriber.")
    }

    fun end() {
        val api = AstahAPI.getAstahAPI()
        val projectAccessor = api.projectAccessor

        println("Stopping subscriber...")
        mqttSubscriber.close()
        println("Stopped subscriber.")

        println("Stopping publisher...")
        projectAccessor.removeProjectEventListener(projectChangedListener)
        println("Stopped publisher.")
    }
}