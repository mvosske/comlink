package org.tnsfit.dragon.comlink

import android.os.AsyncTask
import android.util.Log
import android.widget.Button
import java.io.IOException
import java.net.*
import java.util.*

/**
 * Created by dragon on 08.10.16.
 *
 */

class SendAgentAsyncTask(private val button: Button, private val server: ServerSocket = ServerSocket()): AsyncTask<ByteArray, Int, Int>() {

    private val mSockets: MutableList<Socket> = ArrayList()

    private inner class Transfer(private val data: ByteArray, private val socket: Socket): Runnable {
        // ToDo Outsource this (individual Transfer) to MatrixService

        init {
            synchronized(this@SendAgentAsyncTask) {
                mSockets.add(socket)
            }
        }
        override fun run() {

            val outStream = socket.outputStream
            try {
                outStream.write(data)
                outStream.flush()
                outStream.close()
                socket.close()
            } catch (ioe: IOException) {
                Log.e("SendAgentThread", "Something was Wrong: "+ioe.message)
            } finally {
                oneJobFinished(socket)
            }
        }
    }

    override fun onPreExecute() {
        button.text = "0 x gesendet"
        button.setOnClickListener {
            if (!isCancelled) {
                button.text = "Warte auf Abbruch.."
                button.isEnabled = false
                cancel(true)
            }
        }

        if (!server.isBound) {
            server.reuseAddress = true
            server.soTimeout = 6000
            server.bind(InetSocketAddress(24321))
        }
    }

    override fun doInBackground(vararg params: ByteArray?): Int {
        var count = 0
        val theStream = params[0] ?: return 0

        while (!isCancelled) try {

            val socket = server.accept()
            Thread(Transfer(theStream,socket)).start()
            publishProgress(++count)

        } catch (ie: InterruptedException) {
            server.close() // seems never to happen
        } catch (so: SocketException) {
            return count
        } catch (timeout: SocketTimeoutException) {
            // expected to occur, continue loop till cancelled
        } catch (ioe: IOException) {
            ioe.printStackTrace() // unknown if it even gets thrown
        }

        return count
    }

    override fun onProgressUpdate(vararg values: Int?) {
        val number = (values[0] ?: 0).toString()
        button.text = number + " x ausgeliefert."
    }

    override fun onCancelled(result: Int?) {
        if (mSockets.count() == 0) done()
    }

    override fun onPostExecute(result: Int?) {
        // only happens when server-socket gets closed but Async Task not cancelled
    }

    private fun oneJobFinished(socket: Socket) {
        synchronized(this) {
            mSockets.remove(socket)

            if (isCancelled && (mSockets.count() == 0)) {
                server.close()
                done()
            }
        }
    }

    private fun done() {
        button.text = "Send File"
        val l = (button.context as ComlinkActivity).sendListener
        button.setOnClickListener(l)
        button.isEnabled = true
    }

    fun recycle(): SendAgentAsyncTask {
        val newServer = if (server.isClosed) ServerSocket() else server
        return SendAgentAsyncTask(button,newServer)
    }

    fun kill () {
        synchronized(this) {
            server.close()
            for (socket in mSockets) {
                socket.close()
            }
        }
    }
}