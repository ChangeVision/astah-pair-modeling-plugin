/*
 * SocketServer.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.ServerSocket

class SocketServer(private val portNumber: Int, private val reflectTransaction: ReflectTransaction) {
    private lateinit var serverSocket: ServerSocket
    private var isLaunched = false
    private lateinit var job: Job

    fun launch() {
        serverSocket = ServerSocket()
        serverSocket.reuseAddress = true
        serverSocket.bind(InetSocketAddress(portNumber))
        isLaunched = true
        job = GlobalScope.launch {
            kotlin.runCatching {
                withContext(Dispatchers.IO) {
                    serve()
                }
            }
        }
        println("Launched server. localhost:$portNumber")
    }

    fun stop() {
        isLaunched = false
        job.cancel()
        close()
        println("Stopped server.")
    }

    private fun serve() {
        while (isLaunched) {
            try {
                serverSocket.accept().use { socket ->
                    println("Connection established.")
                    socket.getInputStream().use { inputStream ->
                        inputStream.bufferedReader().use { bufferedReader ->
                            while (isLaunched) {
                                val receivedMessage = bufferedReader.readLine() ?: break
                                println("Received: $receivedMessage")
                                val receivedMessageArray = receivedMessage.split("&&")
                                val parentName = receivedMessageArray[0]
                                val childName = receivedMessageArray[1]
                                if (parentName == "" || childName == "") {
                                    break
                                }
                                reflectTransaction.addClass(parentName, childName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                throw e
            }
            println("Connection closed.")
        }
    }

    private fun close() {
        serverSocket.close()
    }
}