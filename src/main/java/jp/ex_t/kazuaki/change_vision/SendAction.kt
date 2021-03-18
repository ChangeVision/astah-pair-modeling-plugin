/*
 * SenderAction.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow
import javax.swing.JOptionPane

class SendAction: IPluginActionDelegate {
    private lateinit var mqttPublisher: MqttPublisher
    private lateinit var projectChangedListener: ProjectChangedListener
    private var isLaunched = false

    @Throws(Exception::class)
    override fun run(window: IWindow) {
        try {
            val api = AstahAPI.getAstahAPI()
            val projectAccessor = api.projectAccessor
            if (!isLaunched) {
                val ipAddress = getHostAddress(window) ?: return
                val topic = "debug/astah" //JOptionPane.showInputDialog("Input topic. (Ex: debug/astah)") ?: return
                val clientId = JOptionPane.showInputDialog("Input child id. (Ex: astah/debug-pub)") ?: return // clientId ... such as Licensed user name
                mqttPublisher = MqttPublisher(ipAddress, topic, clientId)
                projectChangedListener = ProjectChangedListener(mqttPublisher)
                projectAccessor.addProjectEventListener(projectChangedListener)
                println("Publisher ready.")
            } else {
                projectAccessor.removeProjectEventListener(projectChangedListener)
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

    private fun getHostAddressAndPortNumber(window: IWindow): Pair<String, Int>? {
        val ipAddress = getHostAddress(window) ?: return null
        val portNumber = getPortNumber(window) ?: return null
        return Pair(ipAddress, portNumber)
    }
}