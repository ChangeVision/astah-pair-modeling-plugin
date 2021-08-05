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
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.Window
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

                val confirmDialog = ConfirmDialog(window.parent, "AIKOTOBA", Dialog.ModalityType.DOCUMENT_MODAL, topic)

                val clientId = UUID.randomUUID().toString()
                pairModeling.create(topic, clientId, config.conf.brokerAddress, config.conf.brokerPortNumber)
                confirmDialog.isVisible = true
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

private class ConfirmDialog(owner: Window, title: String, modal: ModalityType, topic: String) :
    JDialog(owner, title, modal) {
    init {
        val dialogPanel = getDialogPanel(topic)
        contentPane.add(dialogPanel)
        isResizable = false
        pack()
        setLocationRelativeTo(parent)
    }

    private fun getConfirmButtonPanel(): JPanel {
        val confirmButtonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        val confirmButton = JButton("OK")
        confirmButton.addActionListener { dispose() }
        confirmButtonPanel.add(confirmButton)
        return confirmButtonPanel
    }

    private fun getDialogPanel(topic: String): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        val confirmButtonPanel = getConfirmButtonPanel()
        val upperText = JLabel("Your AIKOTOBA is")
        val centerText = JTextField(topic)
        centerText.isEditable = false
        val lowerText = JLabel("Tell collaborator this AIKOTOBA and enjoy pair modeling!")
        panel.add(upperText)
        panel.add(centerText)
        panel.add(lowerText)
        panel.add(confirmButtonPanel)
        return panel
    }
}