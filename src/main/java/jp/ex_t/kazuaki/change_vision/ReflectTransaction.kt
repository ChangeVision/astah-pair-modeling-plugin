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
import com.change_vision.jude.api.inf.model.IModel
import com.change_vision.jude.api.inf.project.ProjectAccessor

class ReflectTransaction(private val projectChangedListener: ProjectChangedListener? = null) {
    private val api: AstahAPI = AstahAPI.getAstahAPI()
    private val projectAccessor: ProjectAccessor = api.projectAccessor

    @Throws(BadTransactionException::class)
    fun add(parentName: String, name: String) {
        val transactionManager = projectAccessor.transactionManager
        val modelEditorFactory = projectAccessor.modelEditorFactory
        val basicModelEditor = modelEditorFactory.basicModelEditor
        val parent = projectAccessor.findElements(IModel::class.java, parentName).first() as IModel
        try {
            if (projectChangedListener != null)
                projectAccessor.removeProjectEventListener(projectChangedListener)
            transactionManager.beginTransaction()
            basicModelEditor.createClass(parent, name)
            transactionManager.endTransaction()
        } catch (e: BadTransactionException) {
            transactionManager.abortTransaction()
            throw e
        } finally {
            if (projectChangedListener != null)
                projectAccessor.addProjectEventListener(projectChangedListener)
        }
    }
}