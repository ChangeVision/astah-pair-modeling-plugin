/*
 * PairModeling.kt - pair-modeling
 * Copyright © 2022 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import jp.ex_t.kazuaki.change_vision.apply_transaction.SystemMessageReceiver
import jp.ex_t.kazuaki.change_vision.apply_transaction.TransactionReceiver
import jp.ex_t.kazuaki.change_vision.event_listener.ProjectChangedListener
import jp.ex_t.kazuaki.change_vision.network.EntityTable
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

    @Throws(MqttException::class)
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
        val entityTable = EntityTable()
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        val mqttPublisher = MqttPublisher(brokerAddress, brokerPortNumber, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(entityTable, mqttPublisher)
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
            projectAccessor.removeProjectEventListener(projectChangedListener)
            logger.error("MQTT exception.", e)
            throw e
        }

        val topicTransactionSubscriber = "$topicTransaction/#"
        val transactionReceiver = TransactionReceiver(entityTable, projectChangedListener, clientId)
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
            SystemMessageReceiver(entityTable, mqttPublisher, clientId, topic, brokerAddress, brokerPortNumber)
        mqttSubscriber.subscribe(
            topicSystemMessageSubscriber,
            systemMessageReceiver,
        )
        logger.debug("Subscribed: $brokerAddress:$topicSystemMessage ($clientId)")
        logger.info("Launched system message subscriber.")

        isLaunched = true
    }

    @ExperimentalSerializationApi
    @Throws(MqttException::class, TimeoutCancellationException::class, CancellationException::class)
    suspend fun join(
        topicBase: String,
        clientId: String,
        brokerAddress: String,
        brokerPortNumber: Int
    ) {
        val commitId = getCommitId()
        val topic = "$topicBase-$commitId"
        check(isLaunched.not()) { "Pair modeling has already launched." }

        if (projectAccessor.hasProject()) {
            logger.debug("Already project is opened.")
            check(projectAccessor.isProjectModified.not()) { "Project is already modified." }
            projectAccessor.close()
        }

        val topicTransaction = "$topic/transaction"
        val entityTable = EntityTable()

        try {
            logger.debug("Launching subscriber...")
            mqttSubscriber =
                MqttSubscriber(
                    brokerAddress,
                    brokerPortNumber,
                    clientId,
                )
        } catch (e: MqttException) {
            logger.error("MQTT exception.", e)
            throw e
        }

        // 差分の同期を始める
        logger.debug("Launching publisher...")
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        val mqttPublisher = MqttPublisher(brokerAddress, brokerPortNumber, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(entityTable, mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        logger.debug("Publisher launched: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched publisher.")

        logger.debug("Launching subscriber...")
        val topicTransactionSubscriber = "$topicTransaction/#"
        val transactionReceiver = TransactionReceiver(entityTable, projectChangedListener, clientId)
        mqttSubscriber.subscribe(topicTransactionSubscriber, transactionReceiver)
        logger.debug("Subscribed: $brokerAddress:$topicTransaction ($clientId)")
        logger.info("Launched subscriber.")

        // システムメッセージのsubscriberを用意
        val topicSystemMessage = "$topic/system"
        val topicSystemMessageSubscriber = "$topicSystemMessage/#"
        val systemMessageReceiver =
            SystemMessageReceiver(entityTable, mqttPublisher, clientId, topic, brokerAddress, brokerPortNumber)
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