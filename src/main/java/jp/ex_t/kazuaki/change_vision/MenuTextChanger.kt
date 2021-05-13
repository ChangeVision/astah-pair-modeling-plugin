/*
 * MenuTextChanger.kt - pair-modeling
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.AstahAPI
import java.util.*
import javax.swing.JMenu
import javax.swing.JMenuItem

class MenuTextChanger {
    private val api = AstahAPI.getAstahAPI()
    private val resourceBundle = ResourceBundle.getBundle("plugin")
    private val toolMenuId = "managementview.menu.tool"
    private val pairModelingId = "$toolMenuId.pair-modeling"
    private val actionTextSet =
        arrayListOf(
            ActionTextSet(
                "jp.ex_t.kazuaki.change_vision.CreateRoomAction",
                "menu_label_create_room",
                "menu_label_exit_room",
            ),
            ActionTextSet(
                "jp.ex_t.kazuaki.change_vision.JoinRoomAction",
                "menu_label_join_room",
                "menu_label_exit_room",
            ),
        )

    private fun getPairModelingMenu(): JMenu? {
        val toolMenu = api.viewManager.mainFrame.jMenuBar.components.find { it.name == toolMenuId } as JMenu? ?: run {
            logger.error("Tool menu not found.")
            return null
        }
        val pairModelingMenu = toolMenu.menuComponents.find { it.name == pairModelingId } as JMenu? ?: run {
            logger.error("Pair modeling menu not found.")
            return null
        }
        return pairModelingMenu
    }

    fun setBefore() {
        val pairModelingMenu = getPairModelingMenu() ?: run {
            return
        }
        actionTextSet.forEach { actionText ->
            val action =
                pairModelingMenu.menuComponents.find { it.name == "$pairModelingId.${actionText.id}" } as JMenuItem?
                    ?: run {
                        logger.error("Pair modeling menu ${actionText.id} not found.")
                        return
                    }
            action.text = resourceBundle.getString(actionText.beforeTextId)
        }
    }

    fun setAfter() {
        val pairModelingMenu = getPairModelingMenu() ?: run {
            return
        }
        actionTextSet.forEach { actionText ->
            val action =
                pairModelingMenu.menuComponents.find { it.name == "$pairModelingId.${actionText.id}" } as JMenuItem?
                    ?: run {
                        logger.error("Pair modeling menu ${actionText.id} not found.")
                        return
                    }
            action.text = resourceBundle.getString(actionText.afterTextId)
        }
    }

    companion object : Logging {
        private val logger = logger()
        private var instance: MenuTextChanger? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: MenuTextChanger().also { instance = it }
        }
    }
}

data class ActionTextSet(
    val id: String,
    val beforeTextId: String,
    val afterTextId: String,
)
