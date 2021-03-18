/*
 * ReceiveAction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow
import javax.swing.JOptionPane

class ReceiveAction: IPluginActionDelegate {
    private lateinit var socketServer: SocketServer
    private lateinit var mqttSubscriber: MqttSubscriber
    private lateinit var reflectTransaction: ReflectTransaction
    private var isLaunched = false

    @Throws(Exception::class)
    override fun run(window: IWindow) {
        try {
            if (!isLaunched) {
                val ipAddress = getHostAddress(window) ?: return
                reflectTransaction = ReflectTransaction()
                val topic = "debug/astah" //JOptionPane.showInputDialog("Input topic. (Ex: debug/astah)") ?: return
                val clientId = JOptionPane.showInputDialog("Input child id. (Ex: astah/debug-sub)") ?: return // clientId ... such as Licensed user name
                mqttSubscriber = MqttSubscriber(ipAddress, topic, clientId, reflectTransaction)
                mqttSubscriber.subscribe()
            } else {
                mqttSubscriber.close()
            }
            isLaunched = !isLaunched
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(window.parent, "Exception occurred.", "Alert", JOptionPane.ERROR_MESSAGE)
            println(e.message)
            throw e
        }
    }

    private fun getPortNumber(window: IWindow): Int? {
        val portNumber = (JOptionPane.showInputDialog(window.parent, "Input server port.") ?: return null).toInt()
        if ((0 > portNumber) or (65535 < portNumber)) {
            val message = "Port number must be an integer and be 0 - 65535."
            JOptionPane.showMessageDialog(window.parent, message, "Port number error", JOptionPane.WARNING_MESSAGE)
            return null
        }
        return portNumber
    }

    private fun getHostAddress(window: IWindow): String? {
        val ipAddress = JOptionPane.showInputDialog(window.parent, "Input IP address or \"localhost\"") ?: return null
        val ipAddressPattern = Regex("""^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])${'$'}""")
        if (!ipAddressPattern.matches(ipAddress) && ipAddress != "localhost") {
            val message = "IP address must be IPv4 address."
            JOptionPane.showMessageDialog(window.parent, message, "IP address error", JOptionPane.WARNING_MESSAGE)
            return null
        }
        return ipAddress
    }
}