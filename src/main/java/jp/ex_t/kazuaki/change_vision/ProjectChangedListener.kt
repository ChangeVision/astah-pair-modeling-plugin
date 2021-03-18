/*
 * ProjectChangedListener.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import com.change_vision.jude.api.inf.model.IClass
import com.change_vision.jude.api.inf.model.IModel
import com.change_vision.jude.api.inf.project.ProjectEvent
import com.change_vision.jude.api.inf.project.ProjectEventListener

class ProjectChangedListener(private val mqttPublisher: MqttPublisher): ProjectEventListener {
    override fun projectChanged(e: ProjectEvent) {
        println("===== Transaction detected =====")
        val projectEditUnit = e.projectEditUnit.filter { it.entity != null }
        projectEditUnit
            .forEach {
                val operation = Operation.values()[it.operation]
                when (val entity = it.entity) {
                    is IClass -> {
                        if (operation == Operation.ADD) {
                            val model = entity.owner as IModel
                            mqttPublisher.publish("${model.name}&&${entity.name}")
                        }
                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IClass)")
                    }
//                    is IAssociation -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IAssociation)")
//                    }
//                    is IAttribute -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IAttribute)")
//                    }
//                    is IOperation -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IOperation)")
//                    }
//                    is IModel -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.name}(IModel)")
//                    }
//                    is IPresentation -> {
//                        println("Op: ${Operation.values()[it.operation]} -> ${entity.label}(IPresentation)")
//                    }
                    else -> {
                        println("Op: ${Operation.values()[it.operation]} -> ${entity}(Unknown)")
                    }
                }
            }
    }

    override fun projectOpened(p0: ProjectEvent) {}
    override fun projectClosed(p0: ProjectEvent) {}
}