/*
 * ReflectTransaction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.exception.BadTransactionException
import com.change_vision.jude.api.inf.project.ProjectAccessor
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import jp.ex_t.kazuaki.change_vision.diagram.ClassDiagramApplyTransaction
import jp.ex_t.kazuaki.change_vision.diagram.MindmapDiagramApplyTransaction
import javax.swing.SwingUtilities

class ReflectTransaction(private val projectChangedListener: ProjectChangedListener) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor
    private val classDiagramApplyTransaction = ClassDiagramApplyTransaction()
    private val mindmapDiagramApplyTransaction = MindmapDiagramApplyTransaction()

    @Throws(UnExpectedException::class)
    fun transact(transaction: Transaction) {
        SwingUtilities.invokeLater {
            val transactionManager = projectAccessor.transactionManager
            try {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                logger.debug("Start transaction.")
                transactionManager.beginTransaction()
                classDiagramApplyTransaction.apply(transaction)
                mindmapDiagramApplyTransaction.apply(transaction)
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

    companion object: Logging {
        private val logger = logger()
    }
}