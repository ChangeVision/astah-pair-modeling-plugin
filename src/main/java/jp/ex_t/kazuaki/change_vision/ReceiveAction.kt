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
    private lateinit var reflectTransaction: ReflectTransaction
    private var isLaunched = false

    @Throws(Exception::class)
    override fun run(window: IWindow) {
        try {
            if (!isLaunched) {
                val portNmber = getPortNumber(window) ?: return
                reflectTransaction = ReflectTransaction()
                socketServer = SocketServer(portNmber, reflectTransaction)
                socketServer.launch()
            } else {
                socketServer.stop()
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
}