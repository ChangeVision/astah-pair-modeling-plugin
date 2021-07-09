/*
 * TransactionReceiver.kt - pair-modeling
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
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.eclipse.paho.client.mqttv3.MqttMessage
import javax.swing.SwingUtilities

class TransactionReceiver(
    private val entityLUT: EntityLUT, private val projectChangedListener: ProjectChangedListener,
    clientId: String
) :
    Receiver(clientId) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor
    private val commonApplyTransaction = CommonApplyTransaction(entityLUT)
    private val classDiagramApplyTransaction = ClassDiagramApplyTransaction(entityLUT)
    private val mindmapDiagramApplyTransaction = MindmapDiagramApplyTransaction(entityLUT)
    private val stateMachineDiagramApplyTransaction = StateMachineDiagramApplyTransaction(entityLUT)

    @ExperimentalSerializationApi
    override fun messageArrived(topic: String, message: MqttMessage) {
        if (isReceivedMyself(topic)) return
        val receivedMessage = Cbor.decodeFromByteArray<Transaction>(message.payload)
        logger.debug("Received: $receivedMessage ($topic)")
        transact(receivedMessage)
    }

    @Throws(UnExpectedException::class)
    private fun transact(transaction: Transaction) {
        SwingUtilities.invokeLater {
            logger.debug("Received: $transaction")
            val transactionManager = projectAccessor.transactionManager
            try {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                val selectedElements = api.viewManager.diagramViewManager.selectedPresentations
                // Project creation
                transaction.operations.filterIsInstance<CreateProject>().forEach {
                    validateAndCreateProject(it)
                }
                logger.debug("Start transaction.")
                transactionManager.beginTransaction()
                commonApplyTransaction.apply(
                    transaction.operations.filterIsInstance<CommonOperation>()
                )
                classDiagramApplyTransaction.apply(
                    transaction.operations.filterIsInstance<ClassDiagramOperation>()
                )
                mindmapDiagramApplyTransaction.apply(
                    transaction.operations.filterIsInstance<MindmapDiagramOperation>()
                )
                stateMachineDiagramApplyTransaction.apply(
                    transaction.operations.filterIsInstance<StateMachineDiagramOperation>()
                )
                transactionManager.endTransaction()
                logger.debug("Finished transaction.")
                api.viewManager.diagramViewManager.select(selectedElements)
            } catch (e: BadTransactionException) {
                transactionManager.abortTransaction()
                logger.warn("Failed to transaction.", e)
                throw UnExpectedException()
            } finally {
                projectAccessor.addProjectEventListener(projectChangedListener)
            }
        }
    }

    private fun validateAndCreateProject(operation: CreateProject) {
        if (operation.name.isNotEmpty() && operation.id.isNotEmpty()) {
            createProject(operation.name, operation.id)
        }
    }

    private fun createProject(name: String, id: String) {
        logger.debug("Create project.")
        projectAccessor.create()
        projectAccessor.project.name = name

        entityLUT.entries.add(Entry(projectAccessor.project.id, id))
    }

    companion object : Logging {
        private val logger = logger()
    }
}