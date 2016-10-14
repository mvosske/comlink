package org.tnsfit.dragon.comlink

/**
 * Created by dragon on 08.10.16.
 */


data class MessagePacket (val type:String, val message:String) {
    fun pack(): ByteArray {
        var result = type

        result += message.replace(';',',') + ";" // semicolon is the devider/end of message

        return result.toByteArray(Charsets.UTF_8)
    }
}


fun MessagePacketFactory(input:ByteArray): MessagePacket {
    val stringed = input.toString(Charsets.UTF_8)
    val type = stringed.subSequence(0,10).toString()
    val messageEnding = stringed.indexOf(';')
    var  message = ""

    if (messageEnding > 10) {
        message = stringed.substring(10,messageEnding)
    }

    when (type) {
        Matrix.const.HELLO -> return MessagePacket(Matrix.const.HELLO,message)
        Matrix.const.PING -> return MessagePacket(Matrix.const.PING,message)
        Matrix.const.SEND -> return MessagePacket(Matrix.const.SEND,message)
        else -> return MessagePacket(Matrix.const.HELLO,"\"Hello\" bekommen.")
    }
}

