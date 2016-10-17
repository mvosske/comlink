package org.tnsfit.dragon.comlink.matrix

import android.net.Uri

/**
 * @author eric.neidhardt on 17.10.2016.
 */

/**
 * Interfaces Not actually required to define, but it helps to get a clear view.
 */

data class MessageEvent(val arbitraryData: String)
interface MessageEventListener {
	fun onMessageEvent(event: MessageEvent)
}

data class PingEvent(val x: Int, val y: Int)
interface PingEventListener {
	fun onPingEvent(event: PingEvent)
}

// data class MessagePacket has its own File
interface SendEventListener {
	fun onSendEvent(messagePacket: MessagePacket)
}

data class ImageEvent(val eventUri: Uri)
interface ImageEventListener {
	fun onImageEvent(eventUri: Uri)
}