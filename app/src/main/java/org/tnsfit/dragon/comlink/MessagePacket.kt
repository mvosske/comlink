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

    fun checkOK(): Boolean {
        return when (type) {
            Matrix.const.SEND,
            Matrix.const.MESSAGE -> { if (message.equals("")) false else true}
            Matrix.const.PING -> {if (message.contains(",")) true else false}
            Matrix.const.HELLO,
            Matrix.const.ANSWER -> { true }
            else -> false
        }
    }
}


fun MessagePacketFactory(input:ByteArray): MessagePacket {
    val stringed = input.toString(Charsets.UTF_8)
    val type:String = stringed.subSequence(0,10).toString()
    val messageEnding = stringed.indexOf(';')
    var  message = ""

    if (messageEnding > 10) {
        message = stringed.substring(10,messageEnding)
    } else {
        return MessagePacket("","")
    }
    return MessagePacket(type,message)

}

