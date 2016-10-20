package org.tnsfit.dragon.comlink.matrix

import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.tnsfit.dragon.comlink.StatusTracker
import org.tnsfit.dragon.comlink.misc.registerIfRequired
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.*

/**
 * Created by dragon on 20.10.16.
 * Öffnet einen Serversocket um ein neues Bild (bzw. Handout) zu liefern
 *
 */

class SendAgent(private val socketPool: SocketPool, private val dataInputStream: InputStream): Thread(), StatusEventListener {

    private val eventBus = EventBus.getDefault()
    private val data: ByteArray
    val server:ServerSocket = ServerSocket()
    var isRunning: Boolean = false
    var count: Int = 0

    init {
        data = readBytes(dataInputStream)
    }

    private inner class Transfer(private val socket: Socket): Runnable {
        override fun run() {
            socketPool.registerSocket(socket)
            val outStream = socket.outputStream
            try {
                outStream.write(data)
                outStream.flush()
                outStream.close()
                socket.close()
                count++
                eventBus.post(StatusEvent(StatusTracker.PROGRESS, count.toString()))
            } catch (ioe: IOException) {
                Log.e("SendAgentThread", "Something was Wrong: "+ioe.message)
                socket.close()
            } finally {
                socketPool.unregisterSocket(socket)
            }
        }
    }

    override fun run() {
        isRunning = true
        eventBus.registerIfRequired(this)
        eventBus.post(StatusEvent(StatusTracker.SENDING))

        server.reuseAddress = true
        server.soTimeout = 6000
        server.bind(InetSocketAddress(24321))
        socketPool.registerSocket(server)

        eventBus.post(MessagePacket(MatrixConnection.SEND,"handout",MessagePacket.COMLINK))

        while (isRunning) try {

            val transferSocket = server.accept()
            Thread(Transfer(transferSocket)).start()

        } catch (so: SocketException) {
            // normal Abmelden, vmtlö wurde nur der Socket weggesch lossen
        } catch (timeout: SocketTimeoutException) {
            // expected to occur, continue loop till cancelled
        } catch (ioe: IOException) {
            ioe.printStackTrace() // unknown if it even gets thrown
        }
        isRunning = false
        server.close()
        socketPool.unregisterSocket(server)
        eventBus.unregister(this)
        eventBus.post(StatusEvent(StatusTracker.IDLE))
    }

    fun readBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        try {

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)

            var len: Int
            while (true) {
                len = inputStream.read(buffer)
                if (len == -1) break
                byteBuffer.write(buffer, 0, len)
            }
        } catch (e: IOException) {
            return ByteArray(0)
        }

        return byteBuffer.toByteArray()
    }

    @Subscribe
    override fun onStatusEvent(statusEvent: StatusEvent) {
        if (statusEvent.status == StatusTracker.ABORTING) {
            isRunning = false
        }
    }

}
