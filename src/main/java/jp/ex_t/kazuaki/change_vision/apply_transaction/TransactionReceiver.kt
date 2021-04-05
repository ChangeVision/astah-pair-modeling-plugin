/*
 * ReflectTransaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.apply_transaction

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.event_listener.ProjectChangedListener
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.ClassDiagramOperation
import jp.ex_t.kazuaki.change_vision.network.IReceiver
import jp.ex_t.kazuaki.change_vision.network.MindmapDiagramOperation
import jp.ex_t.kazuaki.change_vision.network.Transaction
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.swing.SwingUtilities

class TransactionReceiver(private val projectChangedListener: ProjectChangedListener): IReceiver {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor
    private val classDiagramApplyTransaction = ClassDiagramApplyTransaction()
    private val mindmapDiagramApplyTransaction = MindmapDiagramApplyTransaction()

    @ExperimentalSerializationApi
    override fun receive(message: MqttMessage) {
        val receivedMessage = Cbor.decodeFromByteArray<Transaction>(message.payload)
        logger.debug("Message: $receivedMessage")
        transact(receivedMessage)
    }

    @Throws(UnExpectedException::class)
    fun transact(transaction: Transaction) {
        SwingUtilities.invokeLater {
            val transactionManager = projectAccessor.transactionManager
            try {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                logger.debug("Start transaction.")
                transactionManager.beginTransaction()
                classDiagramApplyTransaction.apply(
                    transaction.operations.filterIsInstance<ClassDiagramOperation>()
                )
                mindmapDiagramApplyTransaction.apply(
                    transaction.operations.filterIsInstance<MindmapDiagramOperation>()
                )
                transactionManager.endTransaction()
                logger.debug("Finished transaction.")
                api.viewManager.diagramViewManager.select(emptyArray())
            } catch (e: BadTransactionException) {
                transactionManager.abortTransaction()
                logger.warn("Failed to transaction.", e)
                throw UnExpectedException()
            } finally {
                projectAccessor.addProjectEventListener(projectChangedListener)
            }
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}