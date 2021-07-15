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
import java.util.*
import javax.swing.JOptionPane
import javax.swing.ProgressMonitor
import kotlin.io.path.ExperimentalPathApi
import kotlin.math.min

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

                val progressMonitor = ProgressMonitor(window.parent, "Connecting...", "", 0, 100)
                progressMonitor.millisToDecideToPopup = 0
                progressMonitor.millisToPopup = 0
                progressMonitor.setProgress(0)
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
                        progressMonitor.close()
                        logger.debug("Canceled by system.")
                        JOptionPane.showMessageDialog(
                            window.parent,
                            "No response to the connection request.\nConfirm the room is exist.",
                            "Room not found",
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
                scope.launch(Dispatchers.Swing) {
                    for (i in 0..Int.MAX_VALUE) {
                        if (job.isCompleted) {
                            progressMonitor.close()
                            menuTextChanger.setAfterText(menuId)
                            menuTextChanger.disable(menuId)
                            break
                        }
                        if (progressMonitor.isCanceled) {
                            progressMonitor.close()
                            logger.debug("Canceled by progress bar button.")
                            job.cancel()
                            break
                        }
                        delay(200L)
                        progressMonitor.setProgress(min(100, i))
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