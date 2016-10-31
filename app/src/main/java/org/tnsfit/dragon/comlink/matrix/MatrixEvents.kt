package org.tnsfit.dragon.comlink.matrix

import android.net.Uri
import java.net.InetAddress

/**
 * @author eric.neidhardt on 17.10.2016.
 */

/**
 * Interfaces Not actually required to define, but it helps to get a clear view.
 */

// ein Ping, eine Text-Nachricht oder eine Datei-Empfangs-Einladung
interface MessageEventListener {
	fun onMessageEvent(messagePacket: MessagePacket)
}

// ein Bild zur Anzeige steht zur Verfügung
data class ImageEvent(val image: Uri, val source:Int)
interface ImageEventListener {
	fun onImageEvent(imageUri: ImageEvent)
}

data class DownloadEvent(val address: InetAddress, val destination: String, val source: Int)
interface DownloadEventListener {
	fun onDownloadEvent(event: DownloadEvent)
}

data class StatusEvent(val status: Int, val text:String = "")
interface StatusEventListener {
	fun onStatusEvent(statusEvent: StatusEvent)
}

data class KillEvent(val source:Int = MessagePacket.MATRIX)
interface KillEventListener {
	fun onKillEvent(event: KillEvent)
}