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
    private val pairModeling: PairModeling = PairModeling.getInstance()

    @ExperimentalPathApi
    @Throws(UnExpectedException::class)
    override fun run(window: IWindow) {
        try {
            if (pairModeling.isLaunched.not()) {
                val config = Config()
                config.load()
                val topic = getCommitId()
                    ?: "debug/astah" //JOptionPane.showInputDialog("Input topic. (Ex: debug/astah)") ?: return
                val clientId = UUID.randomUUID().toString()

                pairModeling.start(topic, clientId, config.conf.brokerAddress, config.conf.brokerPortNumber)
            } else {
                pairModeling.end()
            }
        } catch (e: UnExpectedException) {
            val message = "Exception occurred."
            JOptionPane.showMessageDialog(window.parent, message, "Alert", JOptionPane.ERROR_MESSAGE)
            logger.error(message, e)
            throw e
        }
    }

    private fun getCommitId(): String? {
        logger.debug("Get commit id.")
        val props = Properties()
        val classLoader: ClassLoader = this.javaClass.classLoader
        logger.debug("Git properties:")
        classLoader.getResourceAsStream("git.properties").use {
            props.load(it)
            props.map { logger.debug("${it.key}: ${it.value}") }
            return props.getProperty("git.commit.id.abbrev")
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}
