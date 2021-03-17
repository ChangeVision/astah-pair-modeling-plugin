/*
 * SocketClient.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import java.io.PrintWriter
import java.net.Socket

class SocketClient(private val ipAddress: String, private val portNumber: Int) {
    private lateinit var socket: Socket

    fun connect() {
        socket = Socket(ipAddress, portNumber)
        println("Connection established.")
    }

    fun write(message: String) {
        socket.getOutputStream().use { outputStream ->
            outputStream.bufferedWriter().use { bufferedWriter ->
                PrintWriter(bufferedWriter).use {
                    it.println(message)
                    it.flush()
                }
            }
        }
    }

    fun read(): String {
        socket.getInputStream().use { inputStream ->
            inputStream.bufferedReader().use {
                return it.readText()
            }
        }
    }

    fun close() {
        socket.close()
        println("Connection closed.")
    }
}