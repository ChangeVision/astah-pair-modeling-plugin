/*
 * ProjectSync.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision.network

import com.change_vision.jude.api.inf.AstahAPI
import jp.ex_t.kazuaki.change_vision.Logging
import jp.ex_t.kazuaki.change_vision.logger
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class ProjectSender(private val topic: String, private val clientId: String, private val brokerAddress: String) :
    IReceiver {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor

    @ExperimentalPathApi
    override fun receive(senderClientId: String, message: MqttMessage) {
        val receiveMessage = message.toString()
        if (receiveMessage == "hello") {
            logger.debug("Exporting project xmi...")
            val projectFile = createTempFile(suffix = ".xmi")
            projectAccessor.exportXMI(projectFile.toString())
            logger.debug("Exported project xmi: $projectFile")
            val topicSender = "$topic/$senderClientId/$clientId"
            val mqttPublisher = MqttPublisher(brokerAddress, topicSender, clientId)
            logger.debug("Reading file...")
            val projectData = projectFile.readBytes()
            logger.debug("Read file.")
            logger.debug("Sending project file...")
            mqttPublisher.publish(projectData)
            logger.debug("Sent project file.")
        }
    }

    companion object : Logging {
        private val logger = logger()
    }
}

class ProjectReceiver : IReceiver {
    private val api = AstahAPI.getAstahAPI()
    private val projectAccessor = api.projectAccessor
    private var receivedFlag = false

    @ExperimentalPathApi
    override fun receive(senderClientId: String, message: MqttMessage) {
        if (receivedFlag) {
            logger.debug("File already received.")
            return
        }
        receivedFlag = true
        logger.debug("Received file.")
        logger.debug("Writing file...")
        val posixFilePermissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--rw-"))
        val projectFile = createTempFile(suffix = ".xmi", attributes = arrayOf(posixFilePermissions))
        projectFile.writeBytes(message.payload)
        logger.debug("Wrote file: $projectFile")
        logger.debug("Importing file...")
        projectAccessor.importXMI(projectFile.toString())
        logger.debug("Imported file.")
    }

    companion object : Logging {
        private val logger = logger()
    }
}