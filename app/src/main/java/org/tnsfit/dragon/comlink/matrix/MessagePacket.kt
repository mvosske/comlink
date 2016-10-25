package org.tnsfit.dragon.comlink.matrix

/**
 * Created by dragon on 08.10.16.
 *
 */


class MessagePacket (val type:String, val message:String, val source:Int = NONE) {

    companion object {
        // Source Types
        val NONE = 0
        val MATRIX = 1
        val COMLINK = 2
    }

    fun pack(): ByteArray {
        var result = type

        result += message.replace(';',',') + ";" // semicolon is the devider/end of message

        return result.toByteArray(Charsets.UTF_8)
    }

    fun checkOK(): Boolean {
        return when (type) {
            MatrixConnection.SEND,
            MatrixConnection.TEXT_MESSAGE -> { if (message.equals("")) false else true}
            MatrixConnection.MARKER,
            MatrixConnection.PING -> {if (message.contains(",")) true else false}
            MatrixConnection.HELLO,
            MatrixConnection.ANSWER -> { true }
            else -> false
        }
    }
}


fun MessagePacketFactory(input:ByteArray, source:Int = MessagePacket.NONE): MessagePacket {
    val stringed = input.toString(Charsets.UTF_8)
    val type:String = stringed.subSequence(0,10).toString()
    val messageEnding = stringed.indexOf(';')

    if (messageEnding > 10) {
        return MessagePacket(type, stringed.substring(10,messageEnding), source)
    } else {
        return MessagePacket("", "", source)
    }
}

