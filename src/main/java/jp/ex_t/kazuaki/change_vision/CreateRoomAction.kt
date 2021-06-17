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
    private val pairModeling: PairModeling = PairModeling.getInstance()
    private val menuTextChanger = MenuTextChanger.getInstance()
    private val menuId = "jp.ex_t.kazuaki.change_vision.CreateRoomAction"

    @ExperimentalPathApi
    @Throws(UnExpectedException::class)
    override fun run(window: IWindow) {
        try {
            if (pairModeling.isLaunched.not()) {
                val config = Config()
                config.load()
                val topic = generatePassword()
                val jTextArea = JTextArea(
                    "Your AIKOTOBA is\n" +
                            topic + "\n" +
                            "Tell collaborator this AIKOTOBA and enjoy pair modeling!"
                )
                val clientId = UUID.randomUUID().toString()
                pairModeling.start(topic, clientId, config.conf.brokerAddress, config.conf.brokerPortNumber)
                JOptionPane.showMessageDialog(window.parent, jTextArea, "AIKOTOBA", JOptionPane.INFORMATION_MESSAGE)
                menuTextChanger.setAfterText(menuId)
                menuTextChanger.disable(menuId)
            } else {
                pairModeling.end()
                menuTextChanger.setBeforeText(menuId)
                menuTextChanger.enable(menuId)
            }
        } catch (e: UnExpectedException) {
            val message = "Exception occurred."
            JOptionPane.showMessageDialog(window.parent, message, "Alert", JOptionPane.ERROR_MESSAGE)
            logger.error(message, e)
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