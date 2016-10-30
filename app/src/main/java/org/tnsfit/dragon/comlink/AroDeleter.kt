package org.tnsfit.dragon.comlink

import android.view.View
import android.view.ViewGroup
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessagePacket

/**
 * Created by dragon on 30.10.16.
 * Listener um Pings, Marker und ggf andere wieder zu löschen
 * OnClick = Ping
 * onLonklick = Marker
 *
 *
 */

class AroDeleter(): View.OnClickListener, View.OnLongClickListener {

    inner class OnTime(val v: View): Runnable {
        override fun run() {
            onClick(v)
        }
    }

    override fun onClick(v: View?) {
        (v?.parent as? ViewGroup)?.removeView(v)
    }

    override fun onLongClick(v: View?): Boolean {
        // nur wenn v ein tag mit Koordinaten hat wars ein Marker
        val tag = v?.tag as? AroCoordinates ?: return false
        EventBus.getDefault().post(MessagePacket(MatrixConnection.REMOVE_MARKER,tag.toString()))
        return true
    }

}
