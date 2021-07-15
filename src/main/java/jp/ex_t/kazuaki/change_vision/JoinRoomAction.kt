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
import java.awt.BorderLayout
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

                val dialog = JDialog(window.parent, "Connecting...")
                dialog.isModal = true
                val progressBar = JProgressBar(0, 100)
                progressBar.value = 0
                progressBar.isIndeterminate = true
                val panel = JPanel(BorderLayout())
                panel.add(progressBar, BorderLayout.NORTH)

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

                val cancelButton = JButton("Cancel")
                cancelButton.addActionListener {
                    dialog.dispose()
                    logger.debug("Canceled by cancel button.")
                    job.cancel()
                }
                panel.add(cancelButton, BorderLayout.EAST)
                dialog.contentPane.add(panel)
                dialog.pack()
                dialog.setLocationRelativeTo(window.parent)
                scope.launch(Dispatchers.Swing) {
                    async {
                        dialog.isVisible = true
                    }
                    for (i in 0..Int.MAX_VALUE) {
                        if (job.isCompleted) {
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