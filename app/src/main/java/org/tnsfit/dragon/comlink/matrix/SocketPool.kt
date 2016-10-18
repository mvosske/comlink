package org.tnsfit.dragon.comlink.matrix

import java.net.Socket
import java.util.*

/**
 * Created by dragon on 18.10.16.
 * verwaltet die Sockets, um sie bei Bedarf alle schließen zu können
 */

class SocketPool {

    val lock = Object()
    private val mSockets: MutableList<Socket> = ArrayList<Socket>()
    var isRunning = true
        private set

    fun registerSocket(socket: Socket): Boolean {
        synchronized(lock) {
            if (isRunning) {
                mSockets.add(socket)
                return true
            } else {
                socket.close()
                return false
            }
        }
    }

    fun unregisterSocket(socket: Socket) {
        synchronized(lock) {
            if (isRunning) mSockets.remove(socket)
        }
    }

    fun stop() {isRunning = false}

    fun closeAllSockets() {
        synchronized(lock) {
            isRunning = false
            for (socket in mSockets) {
                socket.close()
            }
        }
    }

}
