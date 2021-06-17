/*
 * PairModeling.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import jp.ex_t.kazuaki.change_vision.apply_transaction.TransactionReceiver
import jp.ex_t.kazuaki.change_vision.event_listener.ProjectChangedListener
import jp.ex_t.kazuaki.change_vision.network.EntityLUT
import jp.ex_t.kazuaki.change_vision.network.MqttPublisher
import jp.ex_t.kazuaki.change_vision.network.MqttSubscriber
import org.eclipse.paho.client.mqttv3.MqttException
import java.net.SocketTimeoutException

class PairModeling {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    var isLaunched: Boolean = false
        private set

    // TODO: もしプロジェクト全体が欲しいとなった場合はトピックを別で生やす
    // TODO: もしチャットが欲しいとなった場合はトピックを別で生やす
    private lateinit var mqttPublisher: MqttPublisher
    private lateinit var projectChangedListener: ProjectChangedListener
    private lateinit var mqttSubscriber: MqttSubscriber
    private lateinit var transactionReceiver: TransactionReceiver
    private lateinit var entityLUT: EntityLUT

    @Throws(UnExpectedException::class)
    fun start(
        topic: String,
        clientId: String,
        brokerAddress: String,
        brokerPortNumber: Int
    ) {
        check(isLaunched.not()) { "Pair modeling has already launched." }
        val topicTransaction = "$topic/transaction"

        logger.debug("Launching publisher...")
        entityLUT = EntityLUT()
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        mqttPublisher = MqttPublisher(brokerAddress, brokerPortNumber, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(entityLUT, mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        logger.debug("Published: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched publisher.")

        try {
            logger.debug("Launching subscriber...")
            val topicTransactionSubscriber = "$topicTransaction/#"
            transactionReceiver = TransactionReceiver(entityLUT, projectChangedListener)
            mqttSubscriber =
                MqttSubscriber(
                    brokerAddress,
                    brokerPortNumber,
                    topicTransactionSubscriber,
                    clientId,
                    transactionReceiver
                )
            mqttSubscriber.subscribe()
            logger.debug("Subscribed: $brokerAddress:$topicTransaction ($clientId)")
            logger.info("Launched subscriber.")
            isLaunched = isLaunched.not()
        } catch (e: MqttException) {
            if (e.cause is SocketTimeoutException) {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                logger.error("MQTT broker timeout.", e)
                throw UnExpectedException()
            }
        }
    }

    fun end() {
        check(isLaunched) { "Pair modeling has not launched." }

        logger.debug("Stopping subscriber...")
        mqttSubscriber.close()
        logger.info("Stopped subscriber.")

        logger.debug("Stopping publisher...")
        projectAccessor.removeProjectEventListener(projectChangedListener)
        logger.info("Stopped publisher.")
        isLaunched = isLaunched.not()
    }

    companion object : Logging {
        private val logger = logger()
        private var instance: PairModeling? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: PairModeling().also { instance = it }
        }
    }
}