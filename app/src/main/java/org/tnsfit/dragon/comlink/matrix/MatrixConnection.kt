package org.tnsfit.dragon.comlink.matrix

import android.net.Uri
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.*
import java.net.*

/**
 * Created by dragon on 04.10.16.
 *
 */

class MatrixConnection(context: MatrixService): SendEventListener {

    companion object {
        val PING =  "Ping_at_XY"
        val SEND =  "Take_this_"
        val HELLO = "Hi_Chummer"
        val ANSWER = "Welcome__"
        val MESSAGE = "Know_What:"
    }

    val TAG = "UDP Server"

    private var mServer: DatagramSocket
    private var mRunning = false
    private val mContext: MatrixService
    private val mAddressPool = BroadcastAddressPool()
    private val eventBus = EventBus.getDefault()

    init {
        mServer = DatagramSocket(24322)
        mServer.setBroadcast(true)
        mContext = context
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

            send(HELLO,"")

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

    inner class Sender(private val message: MessagePacket): Runnable {
        override fun run() {
            val outSocket = DatagramSocket()
            outSocket.broadcast = true

            for (packet in mAddressPool.getPackets(message.pack())) {
                outSocket.send(packet)
            }
            outSocket.close()
        }
    }

    private inner class MessageProcessor(private val packet: DatagramPacket): Runnable {
        override fun run() {
            if (NetworkInterface.getByInetAddress(packet.address) != null) {
                // came over Loopback, ignore if not in adb Debug Mode
                return
            }

            mAddressPool.confirm(packet.address)

            val content = MessagePacketFactory(packet.data)

            when (content.type) {
                ANSWER -> return
                HELLO -> send(ANSWER,"")
                MESSAGE -> eventBus.post(MessageEvent(content.message))
                SEND -> recieveFile(content.message)
                PING -> receivePing(content.message)
            }
        }

        private fun recieveFile(message: String) {
            val outFile = File(mContext.getExternalFilesDir(null), message)
            try {
                val clientSocket = Socket(packet.address, 24321)
                mContext.registerSocket(clientSocket)
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
                mContext.unregisterSocket(clientSocket)
            } catch (e: IOException) {
                // ToDo Toast Info and let it fail
            }
            eventBus.post(ImageEvent(Uri.fromFile(outFile)))
        }

        fun receivePing(coordsString: String) {
            val coords = coordsString.split(",", limit = 2)
            val percentX: Int = coords[0].toInt()
            val percentY: Int = coords[1].toInt()
            eventBus.post(PingEvent(percentX, percentY))
        }

    }

    fun send(type:String, message:String) {
        send(MessagePacket(type, message))
    }

    fun send(packet: MessagePacket) {
        if (packet.checkOK()) {
            Thread(Sender(packet)).start()
        } else {
            Log.e(TAG, "Wrong call to Matrix.send()")
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

    @Subscribe
    override fun onSendEvent(messagePacket: MessagePacket) {
        // ToDo Change to "threadMode = ThreadMode.ASYNC" and send without additional Threading
        send(messagePacket)
    }
}