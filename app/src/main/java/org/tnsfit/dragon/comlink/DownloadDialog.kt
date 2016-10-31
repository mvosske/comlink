package org.tnsfit.dragon.comlink

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.DownloadEvent
import org.tnsfit.dragon.comlink.matrix.MessagePacket
import java.net.InetAddress

/**
 * Created by dragon on 31.10.16.
 * fragt ob eine Datei empfangen werden soll
 *
 */


class DownloadDialog(context: Context, val address: InetAddress, val requester: String): DialogInterface.OnClickListener {
    private val builder: AlertDialog.Builder

    init {
        builder = AlertDialog.Builder(context)
        builder.setMessage(requester + context.getString(R.string.ui_dialog_question_download))
        builder.setPositiveButton("Ja", this)
        builder.setNegativeButton("Nein", this)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            EventBus.getDefault().post(DownloadEvent(address, requester, MessagePacket.COMLINK))
        }
    }

    fun create(): AlertDialog = builder.create()
}