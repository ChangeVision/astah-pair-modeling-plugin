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
import jp.ex_t.kazuaki.change_vision.apply_transaction.SystemMessageReceiver
import jp.ex_t.kazuaki.change_vision.apply_transaction.TransactionReceiver
import jp.ex_t.kazuaki.change_vision.event_listener.ProjectChangedListener
import jp.ex_t.kazuaki.change_vision.network.EntityLUT
import jp.ex_t.kazuaki.change_vision.network.MqttPublisher
import jp.ex_t.kazuaki.change_vision.network.MqttSubscriber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.eclipse.paho.client.mqttv3.MqttException
import java.net.SocketTimeoutException
import java.util.*

class PairModeling {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    var isLaunched: Boolean = false
        private set

    // TODO: もしプロジェクト全体が欲しいとなった場合はトピックを別で生やす
    // TODO: もしチャットが欲しいとなった場合はトピックを別で生やす
    private lateinit var projectChangedListener: ProjectChangedListener
    private lateinit var mqttSubscriber: MqttSubscriber

    @Throws(UnExpectedException::class)
    fun create(
        topicBase: String,
        clientId: String,
        brokerAddress: String,
        brokerPortNumber: Int
    ) {
        val commitId = getCommitId()
        val topic = "$topicBase-$commitId"
        check(isLaunched.not()) { "Pair modeling has already launched." }
        val topicTransaction = "$topic/transaction"

        logger.debug("Launching publisher...")
        val entityLUT = EntityLUT()
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        val mqttPublisher = MqttPublisher(brokerAddress, brokerPortNumber, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(entityLUT, mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        logger.debug("Publisher launched: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched publisher.")

        try {
            logger.debug("Launching subscriber...")
            mqttSubscriber =
                MqttSubscriber(
                    brokerAddress,
                    brokerPortNumber,
                    clientId,
                )
        } catch (e: MqttException) {
            if (e.cause is SocketTimeoutException) {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                logger.error("MQTT broker timeout.", e)
                throw UnExpectedException()
            }
        }

        val topicTransactionSubscriber = "$topicTransaction/#"
        val transactionReceiver = TransactionReceiver(entityLUT, projectChangedListener, clientId)
        mqttSubscriber.subscribe(
            topicTransactionSubscriber,
            transactionReceiver,
        )
        logger.debug("Subscribed: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched subscriber.")

        // システムメッセージのsubscriberを用意
        val topicSystemMessage = "$topic/system"
        val topicSystemMessageSubscriber = "$topicSystemMessage/#"
        val systemMessageReceiver =
            SystemMessageReceiver(entityLUT, mqttPublisher, clientId, topic, brokerAddress, brokerPortNumber)
        mqttSubscriber.subscribe(
            topicSystemMessageSubscriber,
            systemMessageReceiver,
        )
        logger.debug("Subscribed: $brokerAddress:$topicSystemMessage ($clientId)")
        logger.info("Launched system message subscriber.")

        isLaunched = true
    }

    @ExperimentalSerializationApi
    @Throws(TimeoutCancellationException::class, CancellationException::class)
    suspend fun join(
        topicBase: String,
        clientId: String,
        brokerAddress: String,
        brokerPortNumber: Int
    ) {
        val commitId = getCommitId()
        val topic = "$topicBase-$commitId"
        check(isLaunched.not()) { "Pair modeling has already launched." }
        // TODO: プロジェクトを開いているならば閉じる

        val topicTransaction = "$topic/transaction"
        val entityLUT = EntityLUT()

        try {
            logger.debug("Launching subscriber...")
            mqttSubscriber =
                MqttSubscriber(
                    brokerAddress,
                    brokerPortNumber,
                    clientId,
                )
        } catch (e: MqttException) {
            if (e.cause is SocketTimeoutException) {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                logger.error("MQTT broker timeout.", e)
                throw UnExpectedException()
            }
        }

        // 差分の同期を始める
        logger.debug("Launching publisher...")
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        val mqttPublisher = MqttPublisher(brokerAddress, brokerPortNumber, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(entityLUT, mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        logger.debug("Publisher launched: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched publisher.")

        logger.debug("Launching subscriber...")
        val topicTransactionSubscriber = "$topicTransaction/#"
        val transactionReceiver = TransactionReceiver(entityLUT, projectChangedListener, clientId)
        mqttSubscriber.subscribe(topicTransactionSubscriber, transactionReceiver)
        logger.debug("Subscribed: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched subscriber.")

        // システムメッセージのsubscriberを用意
        logger.debug("Launching system message subscriber...")
        val topicSystemMessage = "$topic/system"
        val topicSystemMessageSubscriber = "$topicSystemMessage/#"
        val systemMessageReceiver =
            SystemMessageReceiver(entityLUT, mqttPublisher, clientId, topic, brokerAddress, brokerPortNumber)
        mqttSubscriber.subscribe(
            topicSystemMessageSubscriber,
            systemMessageReceiver,
        )
        logger.debug("Subscribed: $brokerAddress:$topicSystemMessage ($clientId)")
        logger.info("Launched system message subscriber.")

        logger.debug("Launching system message publisher...")
        val topicSystemMessagePublisher = "$topicSystemMessage/$clientId"
        val systemMessageMqttPublisher =
            MqttPublisher(brokerAddress, brokerPortNumber, topicSystemMessagePublisher, clientId)
        systemMessageMqttPublisher.publish(Cbor.encodeToByteArray("projectsync"))
        try {
            withTimeout(20000L) {
                for (i in 0..20) {
                    if (isStartSync) {
                        break
                    }
                    logger.debug("Wait @${20 - i}")
                    logger.info("Waiting...")
                    delay(1000L)
                }
            }
        } catch (e: TimeoutCancellationException) {
            logger.error("Timeout!!!")
            endProcess()
            throw e
        } catch (e: CancellationException) {
            logger.debug("Canceled by coroutine.")
            endProcess()
            throw e
        }
        logger.debug("Published: $brokerAddress:$topicSystemMessagePublisher ($clientId)")
        logger.info("Launched project sync publisher.")

        isLaunched = true
    }

    fun end() {
        check(isLaunched) { "Pair modeling has not launched." }
        endProcess()
    }

    private fun endProcess() {
        logger.debug("Stopping subscriber...")
        mqttSubscriber.close()
        logger.info("Stopped subscriber.")

        logger.debug("Stopping publisher...")
        projectAccessor.removeProjectEventListener(projectChangedListener)
        logger.info("Stopped publisher.")
        isStartSync = false
        isLaunched = false
    }

    private fun getCommitId(): String? {
        logger.debug("Get commit id.")
        val props = Properties()
        val classLoader: ClassLoader = this.javaClass.classLoader
        logger.debug("Git properties:")
        classLoader.getResourceAsStream("git.properties").use {
            props.load(it)
            props.map { logger.debug("${it.key}: ${it.value}") }
            return props.getProperty("git.commit.id.abbrev")
        }
    }

    var isStartSync = false

    companion object : Logging {
        private val logger = logger()
        private var instance: PairModeling? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: PairModeling().also { instance = it }
        }
    }
}