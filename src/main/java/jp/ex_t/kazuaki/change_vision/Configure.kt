/*
 * Configure.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.*

@ExperimentalPathApi
class Config {
    private val path: Path = Path(System.getProperty("user.home")) / Path(".astah") / Path("plugins") / Path("pair-modeling-plugin")
    private val fileName: Path = Path("config.json")
    private val initialConfig = Conf("", 1883)
    private val configPath = path / fileName
    var conf: Conf = initialConfig
        private set

    fun load() {
        if (configPath.exists()) {
            val jsonRawData = configPath.readText()
            logger.debug("Read data: $jsonRawData")
            conf = Json.decodeFromString(Conf.serializer(), jsonRawData.toString())
            logger.debug("Loaded config.")
        } else {
            logger.debug("Config file not found.")
        }
    }

    fun save() {
        if (path.notExists()) {
            logger.debug("Create directory.")
            path.createDirectories()
        }
        if (configPath.notExists()) {
            logger.debug("Create file.")
            configPath.createFile()
        }
        configPath.writeBytes(Json.encodeToString(Conf.serializer(), conf).toByteArray())
        logger.debug("Saved config.")
    }

    @ExperimentalStdlibApi
    fun setBrokerAddress(brokerAddress: String): Boolean {
        logger.debug("Set broker address.")
        val lowerBrokerAddress = brokerAddress.lowercase()
        val ipv4AddressPattern =
            Regex("""^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)${'$'}""")
        val ipv6AddressPattern = Regex("""^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))${'$'}""")
        val domainPattern = Regex("""^([a-z0-9][a-z0-9-]*[a-z0-9]*\.)+[a-z]{2,}${'$'}""")
        val localhost = "localhost"
        if (ipv4AddressPattern.matches(lowerBrokerAddress) || ipv6AddressPattern.matches(lowerBrokerAddress) || domainPattern.matches(lowerBrokerAddress) || localhost == lowerBrokerAddress) {
            logger.debug("Pattern matched.")
            conf.brokerAddress = lowerBrokerAddress
            return true
        } else {
            logger.debug("Pattern unmatched.")
            return false
        }
    }

    fun setBrokerPortNumber(brokerPortNumber: String): Boolean {
        logger.debug("Set broker port number.")
        try {
            conf.brokerPortNumber = brokerPortNumber.toInt()
            return true
        } catch (e: NumberFormatException) {
            logger.debug("Invalid number format.")
            return false
        }
    }

    companion object: Logging {
        private val logger = logger()
    }
}

@Serializable
data class Conf(
    var brokerAddress: String,
    var brokerPortNumber: Int,
)