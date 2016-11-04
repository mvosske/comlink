package org.tnsfit.dragon.comlink.matrix

import android.net.Uri
import org.greenrobot.eventbus.EventBus
import java.io.*
import java.net.Socket

/**
 * Created by dragon on 31.10.16.
 * Downloaded eine Datei im Hintergrund
 *
 */

class RetrieveAgent(val socketPool: SocketPool, val downloadEvent: DownloadEvent, val workingDirectory: File): Thread() {

    override fun run() {
        var success = true
        val outFile = File(workingDirectory, downloadEvent.destination)
        try {
            val clientSocket: Socket = Socket(downloadEvent.address, 24321)
            socketPool.registerSocket(clientSocket)
            val inStream: BufferedInputStream = clientSocket.inputStream.buffered()
            val outStream = BufferedOutputStream(FileOutputStream(outFile, false))

            val buffer = ByteArray(8192)
            var len: Int

            try {while (true) {
                len = inStream.read(buffer)
                if (len < 0) break
                outStream.write(buffer,0,len)
            }} catch (ioe: IOException) {
                success = false
            }

            outStream.flush()
            inStream.close()
            outStream.close()
            clientSocket.close()
            socketPool.unregisterSocket(clientSocket)
        } catch (e: IOException) {
            success = false
        }
        val eventBus = EventBus.getDefault()
        if (success) {
            eventBus.post(ImageEvent(Uri.fromFile(outFile), MessagePacket.MATRIX))
        } else {
            eventBus.post(MessagePacket(MatrixConnection.TEXT_MESSAGE,"Loading Image failed",MessagePacket.MATRIX))
        }


    }
}
