package org.tnsfit.dragon.comlink

import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.*
import java.net.*

/**
 * Created by dragon on 04.10.16.
 *
 */

class Matrix (callback: Handler, context: ComlinkActivity) {

    object const {
        val PING =  "Ping_at_XY"
        val SEND =  "Take_this_"
        val HELLO = "Hi_Chummer"
        val ANSWER = "Welcome__"
        val MESSAGE = "Know_What:"
    }

    val TAG = "UDP Server"

    private val mUiHandler:Handler
    private var mServer:DatagramSocket
    private var mRunning = false
    private val mContext: ComlinkActivity
    private val mAddressPool = BroadcastAddressPool()

    init {
        mUiHandler = callback
        mServer = DatagramSocket(24322)
        mServer.setBroadcast(true)
        mContext = context

    }

    public inner class Server: Runnable {
        override fun run() {
            synchronized(this@Matrix) {
                mRunning = true;
            }

            // First Time initialisation, to already be able to receive an "Answer" on "Hello"
            if (mServer.isClosed) {
                mServer = DatagramSocket(24322)
            }

            send(const.HELLO,"")

            while (mRunning) try {
                if (mServer.isClosed) {
                    mServer = DatagramSocket(24322)
                }

                val buffer = ByteArray(512)
                val receivePacket = DatagramPacket(buffer,buffer.size)
                mServer.receive(receivePacket)

                Thread(MessageProcessor(receivePacket)).start()

            } catch (se:SocketException) {
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

    public inner class Sender(message: MessagePacket): Runnable {
        private val mMessage: MessagePacket
        init {mMessage = message}

        override fun run() {
            // val destinationAll = getBroadcastAddress()
            // val content = mMessage.pack()
            // val broadcastPacket = DatagramPacket(content,content.size,destinationAll,24322)
            val outSocket = DatagramSocket()
            outSocket.broadcast = true

            for (packet in mAddressPool.getPackets(mMessage.pack())) {
                outSocket.send(packet)
            }

            // outSocket.send(broadcastPacket)
            outSocket.close()
        }
    }

    private inner class MessageProcessor(packet:DatagramPacket): Runnable {
        private val mPacket: DatagramPacket
        init {mPacket = packet}

        override fun run() {
            if (NetworkInterface.getByInetAddress(mPacket.address) != null) {
                // came over Loopback, ignore if not in adb Debug Mode
                return
            }

            mAddressPool.confirm(mPacket.address)

            val content = MessagePacketFactory(mPacket.data)

            when (content.type) {
                const.ANSWER -> return
                const.HELLO -> send(const.ANSWER,"")
                const.MESSAGE -> mUiHandler.post(Runnable { Toast.makeText(mContext,content.message,Toast.LENGTH_SHORT).show() })
                const.SEND -> recieveFile(content.message)
                const.PING -> mUiHandler.post(Runnable { mContext.ping(content.message) })
            }
        }

        private fun recieveFile(message: String) {
            val outFile = File(mContext.getExternalFilesDir(null), message)
            try {
                val clientSocket = Socket(mPacket.address,24321)
                mContext.getSocketManager().add(clientSocket)
                val inStream: BufferedInputStream = clientSocket.inputStream.buffered()
                val outStream = BufferedOutputStream(FileOutputStream(outFile, false))

                val buffer = ByteArray(8192)
                var len = 0

                while (true) {
                    len = inStream.read(buffer)
                    if (len < 0) break
                    outStream.write(buffer,0,len)
                }
                inStream.close()
                outStream.flush()
                outStream.close()
                clientSocket.close()
                mContext.getSocketManager().remove(clientSocket)
            } catch (e: IOException) {
                // ToDo Toast Info and let it fail
            }
            mUiHandler.post(Runnable { mContext.fillImage(outFile) })
        }
    }

    fun ping (coordsString: String) {
        mContext.ping(coordsString)
        send(const.PING,coordsString)
    }

    public fun send(type:String, message:String) {

        val packet = MessagePacket(type, message)

        if (packet.checkOK()) {
            Thread(Sender(packet)).start()
        } else {
            Log.e(TAG, "Wrong call to Matrix.send()")
        }
    }

    public fun startServer() {
        if (mRunning) return
        val t = Thread(Server(), "UdpBroadcstReciever")
        t.start()
    }

    public fun stop() {
        synchronized(this) {
            mRunning = false
            if (!mServer.isClosed) mServer.close()
        }
    }

    private fun getBroadcastAddress(): InetAddress {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        var bcast: InetAddress = Inet4Address.getByAddress(byteArrayOf(127,0,0,1))

        for (card in interfaces) {
            if (!card.isUp) continue
            if (card.name.contains("wlan",true)) {
                val wlanInterfaces = card.interfaceAddresses
                for (wlanX in wlanInterfaces) {
                    if (wlanX.broadcast is Inet4Address) bcast = wlanX.getBroadcast()
                }
            }
        }
        return bcast
    }
}