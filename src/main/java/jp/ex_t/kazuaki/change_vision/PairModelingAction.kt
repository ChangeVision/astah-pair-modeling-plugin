/*
 * PairModelingAction.kt - pair-modeling
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
import kotlin.io.path.ExperimentalPathApi

class PairModelingAction : IPluginActionDelegate {
    private var isLaunched = false
    private lateinit var pairModeling: PairModeling

    @ExperimentalPathApi
    @Throws(UnExpectedException::class)
    override fun run(window: IWindow) {
        try {
            if (!isLaunched) {
                val config = Config()
                config.load()
                val topic = "debug/astah" //JOptionPane.showInputDialog("Input topic. (Ex: debug/astah)") ?: return
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

    private fun getHostAddress(window: IWindow): String? {
        val ipAddress = JOptionPane.showInputDialog(window.parent, "Input IP address or \"localhost\"") ?: return null
        val ipAddressPattern =
            Regex("""^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])${'$'}""")
        if (!ipAddressPattern.matches(ipAddress) && ipAddress != "localhost") {
            val message = "IP address must be IPv4 address."
            JOptionPane.showMessageDialog(window.parent, message, "IP address error", JOptionPane.WARNING_MESSAGE)
            return null
        }
        return ipAddress
    }

    companion object : Logging {
        private val logger = logger()
    }
}
