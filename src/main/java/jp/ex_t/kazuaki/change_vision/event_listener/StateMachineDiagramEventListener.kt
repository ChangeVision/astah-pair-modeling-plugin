/*
 * StateMachineDiagramEventListener.java - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */
package jp.ex_t.kazuaki.change_vision.event_listener

import com.change_vision.jude.api.inf.model.INamedElement
import com.change_vision.jude.api.inf.model.IStateMachineDiagram
import com.change_vision.jude.api.inf.project.ProjectEditUnit
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import jp.ex_t.kazuaki.change_vision.network.*
import kotlinx.serialization.ExperimentalSerializationApi

class StateMachineDiagramEventListener(private val entityLUT: EntityLUT, private val mqttPublisher: MqttPublisher) :
    IEventListener {
    @ExperimentalSerializationApi
    override fun process(projectEditUnit: List<ProjectEditUnit>) {
        logger.debug("Start process")
        val addProjectEditUnit = projectEditUnit.filter { it.operation == Operation.ADD.ordinal }
        val addDiagramUnit = addProjectEditUnit.filter { it.entity is IStateMachineDiagram }
        addDiagramUnit.forEach {
            val createDiagramTransaction =
                Transaction(listOf(createStateMachineDiagram(it.entity as IStateMachineDiagram)))
            ProjectChangedListener.encodeAndPublish(createDiagramTransaction, mqttPublisher)
            return
        }
    }

    private fun createStateMachineDiagram(entity: IStateMachineDiagram): CreateStateMachineDiagram {
        val owner = entity.owner as INamedElement
        val createStateMachineDiagram = CreateStateMachineDiagram(entity.name, owner.name, entity.id)
        entityLUT.entries.add(Entry(entity.id, entity.id))
        logger.debug("$entity(IStateMachineDiagram)")
        return createStateMachineDiagram
    }

    companion object : Logging {
        private val logger = logger()
    }
}