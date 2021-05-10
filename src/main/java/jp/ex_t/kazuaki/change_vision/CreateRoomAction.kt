/*
 * CreateRoomAction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate.UnExpectedException
import com.change_vision.jude.api.inf.ui.IWindow
import java.util.*
import javax.swing.JOptionPane
import javax.swing.JTextArea
import kotlin.io.path.ExperimentalPathApi

class CreateRoomAction : IPluginActionDelegate {
    private var isLaunched = false
    private lateinit var pairModeling: PairModeling

    @ExperimentalPathApi
    @Throws(UnExpectedException::class)
    override fun run(window: IWindow) {
        try {
            if (!isLaunched) {
                val config = Config()
                config.load()
                val topic = generatePassword()
                val jTextArea = JTextArea(
                    "Your AIKOTOBA is\n" +
                            topic + "\n" +
                            "Tell collaborator this AIKOTOBA and enjoy pair modeling!"
                )
                JOptionPane.showMessageDialog(window.parent, jTextArea, "AIKOTOBA", JOptionPane.INFORMATION_MESSAGE)
                val clientId = UUID.randomUUID().toString()
                pairModeling = PairModeling(topic, clientId, config.conf.brokerAddress, config.conf.brokerPortNumber)
                pairModeling.start()
            } else {
                pairModeling.end()
            }
            isLaunched = !isLaunched
        } catch (e: UnExpectedException) {
            val message = "Exception occurred."
            JOptionPane.showMessageDialog(window.parent, message, "Alert", JOptionPane.ERROR_MESSAGE)
            logger.error(message, e)
            throw e
        }
    }

    private fun generatePassword(): String {
        val uuid = UUID.randomUUID().toString()
        return uuid.replace("-", "")
    }

    companion object : Logging {
        private val logger = logger()
    }
}