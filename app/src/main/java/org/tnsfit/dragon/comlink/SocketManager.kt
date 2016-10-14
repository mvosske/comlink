package org.tnsfit.dragon.comlink

import java.net.Socket
import java.util.*

/**
 * Created by dragon on 11.10.16.
 *
 * Manages Sockets to be killed on Activity destroyed to prevent Memory Leak
 *
 */

class SocketManager {

    private val mSockets = ArrayList<Socket>()
    var mClosed = false

    fun add(socket: Socket): Boolean {
        synchronized(this) {
            if (mClosed) return false
            mSockets.add(socket)
            return true
        }
    }

    fun remove(socket: Socket) {
        synchronized(this) {
            if (mClosed) return
            mSockets.remove(socket)
        }
    }

    fun closeAll() {
        synchronized(this) {
            mClosed = true
            for (socket in mSockets) {
                socket.close()
            }
        }
    }

    fun isClosed(): Boolean {
        return mClosed
    }
}
