/*
 * PairModeling.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import jp.ex_t.kazuaki.change_vision.apply_transaction.ReflectTransaction
import jp.ex_t.kazuaki.change_vision.event_listener.ProjectChangedListener
import jp.ex_t.kazuaki.change_vision.network.EntityLUT
import jp.ex_t.kazuaki.change_vision.network.MqttPublisher
import jp.ex_t.kazuaki.change_vision.network.MqttSubscriber

class PairModeling {
    var isLaunched: Boolean = false
        private set
    private val menuTextChanger = MenuTextChanger()

    // TODO: もしプロジェクト全体が欲しいとなった場合はトピックを別で生やす
    // TODO: もしチャットが欲しいとなった場合はトピックを別で生やす
    private lateinit var mqttPublisher: MqttPublisher
    private lateinit var projectChangedListener: ProjectChangedListener
    private lateinit var mqttSubscriber: MqttSubscriber
    private lateinit var reflectTransaction: ReflectTransaction
    private lateinit var entityLUT: EntityLUT

    fun start(
        topic: String,
        clientId: String,
        brokerAddress: String,
        brokerPortNumber: Int
    ) {
        check(isLaunched.not()) { "Pair modeling has already launched." }
        val topicTransaction = "$topic/transaction"
        val api = AstahAPI.getAstahAPI()
        val projectAccessor = api.projectAccessor

        logger.debug("Launching publisher...")
        entityLUT = EntityLUT()
        val topicTransactionPublisher = "$topicTransaction/$clientId"
        mqttPublisher = MqttPublisher(brokerAddress, brokerPortNumber, topicTransactionPublisher, clientId)
        projectChangedListener = ProjectChangedListener(entityLUT, mqttPublisher)
        projectAccessor.addProjectEventListener(projectChangedListener)
        logger.debug("Published: $brokerAddress:$topicTransaction ($clientId")
        logger.info("Launched publisher.")

        logger.debug("Launching subscriber...")
        val topicTransactionSubscriber = "$topicTransaction/#"
        reflectTransaction = ReflectTransaction(entityLUT, projectChangedListener)
        mqttSubscriber =
            MqttSubscriber(brokerAddress, brokerPortNumber, topicTransactionSubscriber, clientId, reflectTransaction)
        mqttSubscriber.subscribe()
        logger.debug("Subscribed: $brokerAddress:$topicTransaction ($clientId")
        logger.info("Launched subscriber.")
        menuTextChanger.setAfter()
        isLaunched = isLaunched.not()
    }

    fun end() {
        check(isLaunched) { "Pair modeling has not launched." }
        val api = AstahAPI.getAstahAPI()
        val projectAccessor = api.projectAccessor

        logger.debug("Stopping subscriber...")
        mqttSubscriber.close()
        logger.info("Stopped subscriber.")

        logger.debug("Stopping publisher...")
        projectAccessor.removeProjectEventListener(projectChangedListener)
        logger.info("Stopped publisher.")
        menuTextChanger.setBefore()
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