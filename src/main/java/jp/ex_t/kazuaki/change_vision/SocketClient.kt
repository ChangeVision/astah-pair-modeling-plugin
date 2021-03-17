/*
 * SocketClient.kt - pair-modeling-prototype
 * Copyright © 2021 HyodaKazuaki.
 *
 * Released under the MIT License.
 * see https://opensource.org/licenses/MIT
 */

package jp.ex_t.kazuaki.change_vision

import java.io.BufferedReader
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.net.Socket

class SocketClient(private val ipAddress: String, private val portNumber: Int) {
    private lateinit var socket: Socket
    private lateinit var inputStream: InputStream
    private lateinit var bufferedReader: BufferedReader
    private lateinit var outputStream: OutputStream
    private lateinit var printWriter: PrintWriter

    fun connect() {
        socket = Socket(ipAddress, portNumber)
        inputStream =socket.getInputStream()
        bufferedReader = inputStream.bufferedReader()
        outputStream = socket.getOutputStream()
        printWriter = PrintWriter(outputStream.bufferedWriter())
        println("Connection established.")
    }

    fun write(message: String) {
        printWriter.println(message)
        printWriter.flush()
    }

    fun read(): String {
        while (inputStream.available() == 0) {}
        return bufferedReader.readLine()
    }

    fun close() {
        bufferedReader.close()
        printWriter.close()
        socket.close()
        println("Connection closed.")
    }
}