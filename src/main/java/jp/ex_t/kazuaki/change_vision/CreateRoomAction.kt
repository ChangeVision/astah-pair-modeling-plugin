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
import java.awt.FlowLayout
import java.util.*
import javax.swing.*
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

                val dialog = JDialog(window.parent, "AIKOTOBA")
                val panel = JPanel()
                panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
                val confirmButton = JButton("OK")
                confirmButton.addActionListener { dialog.dispose() }
                val confirmButtonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
                confirmButtonPanel.add(confirmButton)
                val upperText = JLabel("Your AIKOTOBA is")
                val centerText = JTextField(topic)
                centerText.isEditable = false
                val lowerText = JLabel("Tell collaborator this AIKOTOBA and enjoy pair modeling!")
                panel.add(upperText)
                panel.add(centerText)
                panel.add(lowerText)
                panel.add(confirmButtonPanel)
                dialog.isModal = true
                dialog.contentPane.add(panel)
                dialog.isResizable = false
                dialog.pack()
                dialog.setLocationRelativeTo(window.parent)
                val clientId = UUID.randomUUID().toString()
                pairModeling.create(topic, clientId, config.conf.brokerAddress, config.conf.brokerPortNumber)
                dialog.isVisible = true
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