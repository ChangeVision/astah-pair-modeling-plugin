/*
 * ProjectChangedListener.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.project.ProjectEventListener
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.MqttPublisher
import jp.ex_t.kazuaki.change_vision.Transaction
import jp.ex_t.kazuaki.change_vision.logger
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray

class ProjectChangedListener(mqttPublisher: MqttPublisher) : ProjectEventListener {
    private val classDiagramEventListener: ClassDiagramEventListener = ClassDiagramEventListener(mqttPublisher)
    private val mindmapDiagramEventListener: MindmapDiagramEventListener = MindmapDiagramEventListener(mqttPublisher)

    @ExperimentalSerializationApi
    override fun projectChanged(e: ProjectEvent) {
        logger.debug("===== Transaction detected =====")
        val projectEditUnit = e.projectEditUnit.filter { it.entity != null }
        classDiagramEventListener.process(projectEditUnit)
        mindmapDiagramEventListener.process(projectEditUnit)
    }

    override fun projectOpened(p0: ProjectEvent) {}
    override fun projectClosed(p0: ProjectEvent) {}

    companion object: Logging {
        private val logger = logger()

        @ExperimentalSerializationApi
        fun encodeAndPublish(transaction: Transaction, mqttPublisher: MqttPublisher) {
            val byteArray = Cbor.encodeToByteArray(transaction)
            mqttPublisher.publish(byteArray)
        }
    }
}