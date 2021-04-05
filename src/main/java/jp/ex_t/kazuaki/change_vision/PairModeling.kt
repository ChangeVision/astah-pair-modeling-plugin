/*
 * PairModeling.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import jp.ex_t.kazuaki.change_vision.apply_transaction.TransactionReceiver
import jp.ex_t.kazuaki.change_vision.event_listener.ProjectChangedListener
import jp.ex_t.kazuaki.change_vision.network.*

class PairModeling(topic: String, private val clientId: String, private val brokerAddress: String) {
    private val topicTransaction = "$topic/transaction"
    private val topicSync = "$topic/sync"

    // TODO: もしプロジェクト全体が欲しいとなった場合はトピックを別で生やす
    // TODO: もしチャットが欲しいとなった場合はトピックを別で生やす
    private lateinit var mqttPublisher: MqttPublisher
    private lateinit var projectChangedListener: ProjectChangedListener
    private lateinit var mqttSubscriber: MqttSubscriber

    fun start() {
        val api = AstahAPI.getAstahAPI()
        val projectAccessor = api.projectAccessor

        val projectReceiver = ProjectReceiver()
        val topicProjectReceiveSubscriber = "$topicSync/$clientId/#"
        val receiveSubscriberConfig = MqttSubscriberConfig(topicProjectReceiveSubscriber, 2, projectReceiver)
        val topicSyncSubscriber = "$topicSync/#"
        val projectSender = ProjectSender(topicSync, clientId, brokerAddress)
        val projectSenderConfig = MqttSubscriberConfig(topicSyncSubscriber, 2, projectSender)

        logger.debug("Launching publisher...")
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        mqttPublisher = MqttPublisher(brokerAddress, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        logger.debug("Published: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched publisher.")

        logger.debug("Launching subscriber...")
        val topicTransactionSubscriber = "$topicTransaction/#"
        val transactionReceiver = TransactionReceiver(projectChangedListener)
        val transactionReceiverConfig = MqttSubscriberConfig(topicTransactionSubscriber, 2, transactionReceiver)
        val mqttSubscriberConfigList = listOf(
            receiveSubscriberConfig,
            projectSenderConfig,
            transactionReceiverConfig,
        )
        mqttSubscriber = MqttSubscriber(brokerAddress, mqttSubscriberConfigList, clientId)
        mqttSubscriber.subscribe()
        logger.debug("Subscribed: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched subscriber.")

        syncProject()
    }

    private fun syncProject() {
        logger.debug("Launching publisher...")
        val topicProjectReceivePublisher = "$topicSync/$clientId"
        val topicSyncRequester = MqttPublisher(brokerAddress, topicProjectReceivePublisher, clientId)
        logger.debug("Published: $brokerAddress:$topicSync ($clientId)")
        logger.info("Launched publisher.")

        logger.debug("Requesting project...")
        topicSyncRequester.publish("hello".toByteArray())
        logger.info("Requested project.")
    }

    fun end() {
        val api = AstahAPI.getAstahAPI()
        val projectAccessor = api.projectAccessor

        logger.debug("Stopping subscriber...")
        mqttSubscriber.close()
        logger.info("Stopped subscriber.")

        logger.debug("Stopping publisher...")
        projectAccessor.removeProjectEventListener(projectChangedListener)
        logger.info("Stopped publisher.")
    }

    companion object : Logging {
        private val logger = logger()
    }
}