/*
 * ConfigurationAction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow
import java.awt.Dialog
import java.awt.FlowLayout
import java.awt.Window
import java.awt.event.ActionListener
import javax.swing.*
import kotlin.io.path.ExperimentalPathApi

class ConfigurationAction : IPluginActionDelegate {
    @ExperimentalStdlibApi
    @ExperimentalPathApi
    override fun run(window: IWindow) {
        val config = Config()
        config.load()

        val dialog = ConfigurationDialog(
            window.parent,
            "Pair modeling plugin preference",
            Dialog.ModalityType.DOCUMENT_MODAL,
            config.conf.brokerAddress,
            config.conf.brokerPortNumber
        )
        dialog.addOkButtonActionEventListener {
            val address = dialog.getAddress()
            val port = dialog.getPort()
            if (!config.setBrokerAddress(address)) {
                JOptionPane.showMessageDialog(
                    window.parent,
                    "Broker server address invalid.\nInput correctly broker server address.",
                    "Broker server address invalid.",
                    JOptionPane.WARNING_MESSAGE
                )
            } else if (!config.setBrokerPortNumber(port)) {
                JOptionPane.showMessageDialog(
                    window.parent,
                    "Broker server port number invalid.\nInput correctly broker server port number.",
                    "Broker server port number invalid.",
                    JOptionPane.WARNING_MESSAGE
                )
            } else {
                dialog.dispose()
                config.save()
            }
        }

        dialog.isVisible = true
    }
}

class ConfigurationDialog(owner: Window, title: String, modalityType: ModalityType, address: String, port: Int) :
    JDialog(owner, title, modalityType) {
    private lateinit var okButton: JButton
    private lateinit var addressField: JTextField
    private lateinit var portField: JTextField

    init {
        val titledBorder = BorderFactory.createTitledBorder("MQTT broker")
        val mqttBrokerPanel = JPanel()
        mqttBrokerPanel.border = titledBorder
        mqttBrokerPanel.layout = BoxLayout(mqttBrokerPanel, BoxLayout.Y_AXIS)

        val addressPanel = getAddressPanel(address)
        val portPanel = getPortPanel(port)
        val buttonPanel = getButtonPanel()

        mqttBrokerPanel.add(addressPanel)
        mqttBrokerPanel.add(portPanel)
        mqttBrokerPanel.add(buttonPanel)
        add(mqttBrokerPanel)
        pack()
        setLocationRelativeTo(owner)
    }

    private fun getAddressPanel(address: String): JPanel {
        val addressPanel = JPanel()
        addressPanel.layout = BoxLayout(addressPanel, BoxLayout.X_AXIS)
        val addressLabel = JLabel("Address")
        addressField = JTextField(address)
        addressPanel.add(addressLabel)
        addressPanel.add(addressField)
        return addressPanel
    }

    private fun getPortPanel(port: Int): JPanel {
        val portPanel = JPanel()
        portPanel.layout = BoxLayout(portPanel, BoxLayout.X_AXIS)
        val portLabel = JLabel("Port number")
        portField = JTextField(port.toString())
        portPanel.add(portLabel)
        portPanel.add(portField)
        return portPanel
    }

    private fun getButtonPanel(): JPanel {
        val buttonPanel = JPanel()
        buttonPanel.layout = FlowLayout(FlowLayout.RIGHT)
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener { dispose() }
        okButton = JButton("OK")
        rootPane.defaultButton = okButton
        buttonPanel.add(cancelButton)
        buttonPanel.add(okButton)
        return buttonPanel
    }

    fun addOkButtonActionEventListener(l: ActionListener) {
        okButton.addActionListener(l)
    }

    fun getAddress(): String {
        return addressField.text
    }

    fun getPort(): String {
        return portField.text
    }
}