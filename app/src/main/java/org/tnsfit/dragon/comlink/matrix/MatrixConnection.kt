package org.tnsfit.dragon.comlink.matrix

import android.net.Uri
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.*
import java.net.*

/**
 * Created by dragon on 04.10.16.
 *
 */

class MatrixConnection(val socketPool: SocketPool, val workingDirectory: File): MessageEventListener {

    companion object {
        val PING =  "Ping_at_XY"
        val SEND =  "Take_this_"
        val HELLO = "Hi_Chummer"
        val ANSWER = "Welcome__"
        val TEXT_MESSAGE = "Know_What:"
    }

    val TAG = "UDP Server"

    private var mServer: DatagramSocket
    private var mRunning = false
    private val mAddressPool = BroadcastAddressPool()
    private val eventBus = EventBus.getDefault()

    init {
        mServer = DatagramSocket(24322)
        mServer.broadcast = true
    }

    inner class Server: Runnable {
        override fun run() {
            synchronized(this@MatrixConnection) {
                mRunning = true;
            }

            // First Time initialisation, to already be able to receive an "Answer" on "Hello"
            if (mServer.isClosed) {
                mServer = DatagramSocket(24322)
            }

            eventBus.post(MessagePacket(HELLO,"",MessagePacket.COMLINK))

            while (mRunning) try {
                if (mServer.isClosed) {
                    mServer = DatagramSocket(24322)
                }

                val buffer = ByteArray(512)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                mServer.receive(receivePacket)

                Thread(MessageProcessor(receivePacket)).start()

            } catch (se: SocketException) {
                if (se.message.equals("Socket closed") && !mRunning) {
                    Log.d(TAG, "Socket closed as expected")
                } else {
                    // Maybe react someway, this is not expected
                    Log.d(TAG, "Socket Exception Message: " + se.message)
                    Log.d(TAG, "Socket Exception complete: " + se.toString())
                }
            }
        }
    }

    private inner class MessageProcessor(private val packet: DatagramPacket): Runnable {
        override fun run() {
            if (NetworkInterface.getByInetAddress(packet.address) != null) {
                // came over Loopback, ignore if not in adb Debug Mode
                return
            }

            mAddressPool.confirm(packet.address)

            val content = MessagePacketFactory(packet.data,MessagePacket.MATRIX)

            when (content.type) {
                ANSWER -> return
                HELLO -> eventBus.post(MessagePacket(ANSWER,"",MessagePacket.COMLINK))
                SEND -> receiveFile(content.message)
                PING,
                TEXT_MESSAGE -> eventBus.post(content)
            }
        }

        private fun receiveFile(message: String) {
            val outFile = File(workingDirectory, message)
            try {
                val clientSocket = Socket(packet.address, 24321)
                socketPool.registerSocket(clientSocket)
                val inStream: BufferedInputStream = clientSocket.inputStream.buffered()
                val outStream = BufferedOutputStream(FileOutputStream(outFile, false))

                val buffer = ByteArray(8192)
                var len: Int

                while (true) {
                    len = inStream.read(buffer)
                    if (len < 0) break
                    outStream.write(buffer,0,len)
                }
                inStream.close()
                outStream.flush()
                outStream.close()
                clientSocket.close()
                socketPool.unregisterSocket(clientSocket)
            } catch (e: IOException) {
                // ToDo Toast Info and let it fail
            }
            eventBus.post(ImageEvent(Uri.fromFile(outFile), MessagePacket.MATRIX))
        }
    }

    fun startServer() {
        if (mRunning) return
        val t = Thread(Server(), "UdpBroadcstReciever")
        t.start()
    }

    fun stop() {
        synchronized(this) {
            mRunning = false
            if (!mServer.isClosed) mServer.close()
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onMessageEvent(messagePacket: MessagePacket) {
        if ((messagePacket.source == MessagePacket.MATRIX) || !messagePacket.checkOK()) return

        val outSocket = DatagramSocket()
        outSocket.broadcast = true

        for (packet in mAddressPool.getPackets(messagePacket.pack())) {
            outSocket.send(packet)
        }
        outSocket.close()
    }
}