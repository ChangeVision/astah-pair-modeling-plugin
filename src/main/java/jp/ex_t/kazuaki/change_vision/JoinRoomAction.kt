/*
 * JoinRoomAction.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.ui.IPluginActionDelegate
import com.change_vision.jude.api.inf.ui.IWindow
import java.util.*
import javax.swing.JOptionPane
import kotlin.io.path.ExperimentalPathApi

class JoinRoomAction:IPluginActionDelegate {
    private var isLaunched = false
    private lateinit var pairModeling: PairModeling

    @ExperimentalPathApi
    @Throws(IPluginActionDelegate.UnExpectedException::class)
    override fun run(window: IWindow) {
        try {
            if (!isLaunched) {
                val config = Config()
                config.load()
                val topic = JOptionPane.showInputDialog(window.parent, "Input AIKOTOBA which host tell you.") ?: return
                val clientId = UUID.randomUUID().toString()
                pairModeling = PairModeling(topic, clientId, config.conf.brokerAddress, config.conf.brokerPortNumber)
                pairModeling.start()
            } else {
                pairModeling.end()
            }
            isLaunched = !isLaunched
        } catch (e: IPluginActionDelegate.UnExpectedException) {
            val message = "Exception occurred."
            JOptionPane.showMessageDialog(window.parent, message, "Alert", JOptionPane.ERROR_MESSAGE)
            logger.error(message, e)
            throw e
        }
    }

    companion object: Logging{
        private val logger = logger()
    }
}