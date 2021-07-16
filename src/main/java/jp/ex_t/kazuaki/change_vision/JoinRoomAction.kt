/*
 * JoinRoomAction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import kotlinx.serialization.ExperimentalSerializationApi
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.Window
import java.awt.event.ActionListener
import java.util.*
import javax.swing.*
import kotlin.io.path.ExperimentalPathApi


class JoinRoomAction : IPluginActionDelegate {
    private val pairModeling: PairModeling = PairModeling.getInstance()
    private val menuTextChanger = MenuTextChanger.getInstance()
    private val menuId = "jp.ex_t.kazuaki.change_vision.JoinRoomAction"

    @ExperimentalSerializationApi
    @ExperimentalPathApi
    @Throws(IPluginActionDelegate.UnExpectedException::class)
    override fun run(window: IWindow) {
        try {
            if (pairModeling.isLaunched.not()) {
                val config = Config()
                config.load()
                val topic = JOptionPane.showInputDialog(window.parent, "Input AIKOTOBA which host tell you.") ?: return
                val clientId = UUID.randomUUID().toString()

                val dialog =
                    ProgressBarDialog(window.parent, "Connecting...", Dialog.ModalityType.DOCUMENT_MODAL, 0, 100, true)

                val scope = CoroutineScope(Dispatchers.Default)
                val job = scope.async {
                    try {
                        pairModeling.join(
                            topic,
                            clientId,
                            config.conf.brokerAddress,
                            config.conf.brokerPortNumber
                        )
                    } catch (e: TimeoutCancellationException) {
                        dialog.dispose()
                        logger.debug("Canceled by system.")
                        JOptionPane.showMessageDialog(
                            window.parent,
                            "No response to the connection request.\nConfirm the room is exist.",
                            "Room not found",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }

                dialog.addCancelAction {
                    dialog.dispose()
                    logger.debug("Canceled by cancel button.")
                    job.cancel()
                }

                scope.launch(Dispatchers.Swing) {
                    async {
                        dialog.isVisible = true
                    }
                    for (i in 0..Int.MAX_VALUE) {
                        if (job.isCompleted && job.isCancelled.not()) {
                            logger.debug("Job completed.")
                            menuTextChanger.setAfterText(menuId)
                            menuTextChanger.disable(menuId)
                            dialog.dispose()
                            break
                        }
                        delay(200L)
                    }
                }
            } else {
                pairModeling.end()
                menuTextChanger.setBeforeText(menuId)
                menuTextChanger.enable(menuId)
            }
        } catch (e: IPluginActionDelegate.UnExpectedException) {
            val message = "Exception occurred."
            JOptionPane.showMessageDialog(window.parent, message, "Alert", JOptionPane.ERROR_MESSAGE)
            logger.error(message, e)
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}

private class ProgressBarDialog(
    owner: Window,
    title: String,
    modalityType: ModalityType,
    progressMin: Int,
    progressMax: Int,
    isIndeterminate: Boolean
) : JDialog(owner, title, modalityType) {
    private lateinit var cancelButton: JButton

    init {
        isResizable = false
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val progressBar = getProgressBar(progressMin, progressMax, isIndeterminate)
        val cancelButtonPanel = getCancelButtonPanel()
        panel.add(progressBar)
        panel.add(cancelButtonPanel)
        contentPane.add(panel)
        pack()
        setLocationRelativeTo(owner)
    }

    private fun getProgressBar(min: Int, max: Int, isIndeterminate: Boolean): JProgressBar {
        val progressBar = JProgressBar(min, max)
        progressBar.value = min
        progressBar.isIndeterminate = isIndeterminate
        return progressBar
    }

    private fun getCancelButtonPanel(): JPanel {
        val cancelButtonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        cancelButton = JButton("Cancel")
        cancelButtonPanel.add(cancelButton)
        return cancelButtonPanel
    }

    fun addCancelAction(l: ActionListener) {
        cancelButton.addActionListener(l)
    }

    companion object : Logging {
        private val logger = logger()
    }
}