package org.tnsfit.dragon.comlink.matrix

import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.NetworkInterface
import java.net.SocketException

/**
 * Created by dragon on 04.10.16.
 *
 */

// Todo Implementiere "Auf Schleichfahrt" welches alle nachrichten nur an die schickt, die NICHT das Handout geliefert haben

class MatrixConnection():
        MessageEventListener {

    companion object {
        val PING =   "Ping_at_XY"
        val MARKER = "Mark_at_XY"
        val REMOVE_MARKER = "Delete_XY_"
        // Todo Event für entfernen von Markern
        val SEND =   "Take_this_"
        val HELLO =  "Hi_Chummer"
        val ANSWER = "Welcome__"
        val TEXT_MESSAGE = "Know_What:"
    }

    val TAG = "UDP Server"

    private var mServer: DatagramSocket
    private var isRunning = false
    private val mAddressPool = BroadcastAddressPool()
    private val eventBus = EventBus.getDefault()

    init {
        mServer = DatagramSocket(24322)
        mServer.broadcast = true
    }

    inner class Server: Runnable {
        override fun run() {
            synchronized(this@MatrixConnection) {
                isRunning = true;
            }

            // First Time initialisation, to already be able to receive an "Answer" on "Hello"
            if (mServer.isClosed) {
                mServer = DatagramSocket(24322)
            }

            eventBus.post(MessagePacket(HELLO,"",MessagePacket.COMLINK))

            while (isRunning) try {
                if (mServer.isClosed) {
                    mServer = DatagramSocket(24322)
                }

                val buffer = ByteArray(512)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                mServer.receive(receivePacket)

                Thread(MessageProcessor(receivePacket)).start()

            } catch (se: SocketException) {
                if (se.message.equals("Socket closed") && !isRunning) {
                    Log.d(TAG, "Socket closed as expected")
                } else {
                    // Maybe react someway, this is not expected
                    Log.d(TAG, "Socket Exception Message: " + se.message)
                    Log.d(TAG, "Socket Exception complete: " + se.toString())
                    synchronized(this@MatrixConnection) {
                        isRunning = true;
                    }
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

            val content = MessagePacketFactory(packet.data, MessagePacket.MATRIX)

            when (content.type) {
                ANSWER -> return
                HELLO -> eventBus.post(MessagePacket(ANSWER, "", MessagePacket.COMLINK))
                SEND -> eventBus.post(DownloadEvent(packet.address, content.message, MessagePacket.MATRIX))
                PING,
                MARKER,
                TEXT_MESSAGE -> eventBus.post(content)
            }
        }
    }

    fun startServer() {
        if (isRunning) return
        val t = Thread(Server(), "UdpBroadcstReciever")
        t.start()
    }

    fun stop() {
        synchronized(this) {
            isRunning = false
            if (!mServer.isClosed) mServer.close()
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onMessageEvent(messagePacket: MessagePacket) {
        if (
            (messagePacket.source == MessagePacket.MATRIX) ||
            !messagePacket.checkOK() ||
            !isRunning
        ) return

        val outSocket = DatagramSocket()
        outSocket.broadcast = true

        for (packet in mAddressPool.getPackets(messagePacket.pack())) {
            outSocket.send(packet)
        }
        outSocket.close()
    }
}