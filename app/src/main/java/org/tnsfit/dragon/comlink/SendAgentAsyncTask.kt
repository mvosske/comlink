package org.tnsfit.dragon.comlink

import android.os.AsyncTask
import android.util.Log
import android.widget.Button
import java.io.*
import java.net.*
import java.util.*

/**
 * Created by dragon on 08.10.16.
 *
 */

class SendAgentAsyncTask(button: Button, socketManager: SocketManager, server: ServerSocket = ServerSocket()): AsyncTask<ByteArray, Int, Int>() {

    private val mButton: Button
    private val mWorker: MutableList<Runnable> = ArrayList<Runnable>()
    private val mServer: ServerSocket
    private val mSocketManager: SocketManager

    init {
        mButton = button
        mServer = server
        mSocketManager = socketManager
    }

    private inner class Transfer(data: ByteArray, socket: Socket): Runnable {
        val mSocket:Socket
        val mData: ByteArray

        init {
            mSocket = socket
            mSocketManager.add(mSocket)
            mData = data
        }
        override fun run() {

            val outStream = mSocket.outputStream
            try {
                outStream.write(mData)
                outStream.flush()
                outStream.close()
                mSocket.close()
                mSocketManager.remove(mSocket)
            } catch (ioe: IOException) {
                Log.e("SendAgentThread", "Something was Wrong: "+ioe.message)
            } finally {
                oneJobFinished(this)
            }
        }
    }

    override fun onPreExecute() {
        mButton.text = "0 x gesendet"
        mButton.setOnClickListener {
            if (!isCancelled) {
                mButton.text = "Transfer Abbruch.."
                mButton.isEnabled = false
                cancel(false)
            }
        }

        if (!mServer.isBound) {
            mServer.reuseAddress = true
            mServer.soTimeout = 6000
            mServer.bind(InetSocketAddress(24321))
        }
    }

    override fun doInBackground(vararg params: ByteArray?): Int {
        var count = 0
        val theStream = params[0]
        if (theStream == null) return 0

        while (!isCancelled) try {

            val socket = mServer.accept()
            val worker = Transfer(theStream,socket)
            synchronized(this) {
                mWorker.add(worker)
            }
            Thread(worker).start()
            publishProgress(++count)

        } catch (ie: InterruptedException) {
            mServer.close()
        } catch (so: SocketException) {
            return count
        } catch (timeout: SocketTimeoutException) {
            // expected to occur, continue loop till cancelled
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return count
    }

    override fun onProgressUpdate(vararg values: Int?) {
        val number = (values[0] ?: 0).toString()
        mButton.text = number + " x ausgeliefert."
    }

    override fun onCancelled(result: Int?) {
        done()
    }

    override fun onPostExecute(result: Int?) {
        done()
    }

    private fun oneJobFinished(job: Runnable) {
        synchronized(this) {
            mWorker.remove(job)
            if (isCancelled && (mWorker.count() == 0)) {
                mServer.close()
            }
            if (mSocketManager.isClosed()) {
                cancel(true)
                mServer.close()
            }
        }
    }

    private fun done() {
        mButton.text = "Send File"
        val l = (mButton.context as ComlinkActivity).sendListener
        mButton.setOnClickListener(l)
        mButton.isEnabled = true
    }

    fun recycle(): SendAgentAsyncTask {
        val newServer = if (mServer.isClosed) ServerSocket() else mServer
        return SendAgentAsyncTask(mButton,mSocketManager,newServer)
    }
}