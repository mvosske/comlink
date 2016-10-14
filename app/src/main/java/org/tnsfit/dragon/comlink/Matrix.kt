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

class Matrix (callback: Handler, context: ComlinklActivity) {

    object const {
        val PING =  "Ping_at_XY"
        val SEND =  "Take_this_"
        val HELLO = "Hi_Chummer"

    }

    val TAG = "UDP Server"

    private val mUiHandler:Handler
    private var mServer:DatagramSocket
    private var mRunning = false
    private val mContext: ComlinklActivity

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
            val destinationAll = getBroadcastAddress()
            val content = mMessage.pack()
            val broadcastPacket = DatagramPacket(content,content.size,destinationAll,24322)
            val outSocket = DatagramSocket()
            outSocket.broadcast = true
            outSocket.send(broadcastPacket)
            outSocket.close()
        }
    }

    private inner class MessageProcessor(packet:DatagramPacket): Runnable {
        private val mPacket: DatagramPacket
        init {mPacket = packet}

        override fun run() {
            if (NetworkInterface.getByInetAddress(mPacket.address) != null) {
                Log.d(TAG, "soliloquy")
                return
            }
            // in release should "return",. in debug let it through

            val content = MessagePacketFactory(mPacket.data)

            when (content.type) {
                const.HELLO -> mUiHandler.post(Runnable { Toast.makeText(mContext,content.message,Toast.LENGTH_SHORT).show() })
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

    public fun startServer() {
        DebugMe().getWirelessAdress(mContext);
        if (mRunning) {
            stop()
        } else {
            val t = Thread(Server(), "UdpBroadcstReciever")
            t.start()
        }
    }

    fun ping (coordsString: String) {
        mContext.ping(coordsString)
        send(const.PING,coordsString)
    }

    public fun send(type:String, message:String) {
        val checkOK = when (type) {
            const.HELLO -> true // Maybe false, if its unused
            const.SEND -> { if (message.equals("")) false else true}
            const.PING -> {if (message.contains(",")) true else false}
            else -> false
        }

        if (checkOK) {
            Thread(Sender(MessagePacket(type, message))).start()
        } else {
            Log.e(TAG, "Wrong call to Matrix.send()")
        }
    }

    public fun stop() {
        synchronized(this) {
            mRunning = false
            if (!mServer.isClosed) mServer.close()
        }
    }

    private fun getBroadcastAddress(): InetAddress {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        var bcast: InetAddress = Inet4Address.getLoopbackAddress() as InetAddress

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