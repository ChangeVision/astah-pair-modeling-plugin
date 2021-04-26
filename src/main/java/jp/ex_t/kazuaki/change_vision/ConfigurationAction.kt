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
import javax.swing.JOptionPane
import kotlin.io.path.ExperimentalPathApi

class ConfigurationAction : IPluginActionDelegate {
    @ExperimentalStdlibApi
    @ExperimentalPathApi
    override fun run(window: IWindow) {
        val config = Config()
        config.load()

        val brokerAddress = JOptionPane.showInputDialog(
            window.parent,
            "Input broker server address such as IP address, domain name or \"localhost\".\nIf you want to skip it, click \"Cancel\".",
            config.conf.brokerAddress
        )
        if (brokerAddress != null) {
            if (!config.setBrokerAddress(brokerAddress)) {
                JOptionPane.showMessageDialog(
                    window.parent,
                    "Broker server address invalid.\nInput correctly broker server address.",
                    "Broker server address invalid.",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }
        val brokerPortNumber = JOptionPane.showInputDialog(
            "Input broker server port number.\nIf you want to skip it, click \"Cancel\".",
            config.conf.brokerPortNumber
        )
        if (brokerPortNumber != null) {
            if (!config.setBrokerPortNumber(brokerPortNumber)) {
                JOptionPane.showMessageDialog(
                    window.parent,
                    "Broker server port number invalid.\nInput correctly broker server port number.",
                    "Broker server port number invalid.",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        }

        config.save()
    }
}