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
    private lateinit var socketClient: SocketClient
    private lateinit var projectChangedListener: ProjectChangedListener
    private var isLaunched = false

    @Throws(Exception::class)
    override fun run(window: IWindow) {
        try {
            val api = AstahAPI.getAstahAPI()
            val projectAccessor = api.projectAccessor
            if (!isLaunched) {
                val (ipAddress, portNumber) = getHostAddressAndPortNumber(window) ?: return
                socketClient = SocketClient(ipAddress, portNumber)
                socketClient.connect()
                projectChangedListener = ProjectChangedListener(socketClient)
                projectAccessor.addProjectEventListener(projectChangedListener)
            } else {
                projectAccessor.removeProjectEventListener(projectChangedListener)
                socketClient.close()
            }
            isLaunched = !isLaunched
        } catch (e: IPAddressFormatException) {
            val message = "IP address must be IPv4 address."
            JOptionPane.showMessageDialog(window.parent, message, "IP address error", JOptionPane.WARNING_MESSAGE)
        } catch (e: java.lang.NumberFormatException) {
            val message = "Port number must be an integer and be 0 - 65535."
            JOptionPane.showMessageDialog(window.parent, message, "Port number error", JOptionPane.WARNING_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(window.parent, "Exception occurred.", "Alert", JOptionPane.ERROR_MESSAGE)
            println(e.message)
            throw e
        }
    }

    private fun getHostAddressAndPortNumber(window: IWindow): Pair<String, Int>? {
        val ipAddress = JOptionPane.showInputDialog(window.parent, "Input IP address or \"localhost\"") ?: return null
        val ipAddressPattern = Regex("""^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])${'$'}""")
        if (!ipAddressPattern.matches(ipAddress) && ipAddress != "localhost") throw IPAddressFormatException()
        val portNumber = (JOptionPane.showInputDialog(window.parent, "Input server port.") ?: return null).toInt()
        if ((0 > portNumber) or (65535 < portNumber)) throw NumberFormatException()
        return Pair(ipAddress, portNumber)
    }
}