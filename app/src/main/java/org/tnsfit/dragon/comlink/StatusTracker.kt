package org.tnsfit.dragon.comlink

import android.net.Uri
import org.greenrobot.eventbus.Subscribe
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessageEventListener
import org.tnsfit.dragon.comlink.matrix.MessagePacket
import org.tnsfit.dragon.comlink.matrix.StatusEvent
import java.util.*

/**
 * Created by dragon on 19.10.16.
 *
 * Verfolgt den Status des Senden Buttons, des angezeigten Handouts und der platzierten Marker
 * solange entweder der Service oder die Activity läuft
 *
 */

class StatusTracker(): MessageEventListener,Iterable<AroCoordinates> {
    companion object {
        val STATUS_BLOCKED = 0
        val STATUS_IDLE = 1
        val STATUS_PROGRESS = 2
        val STATUS_ABORTING = 3
    }

    var lastEvent: StatusEvent = StatusEvent(STATUS_BLOCKED)
    var currentHandout: Uri = Uri.parse("android.resource://org.tnsfit.dragon.comlink/drawable/empty_image")
    var name: String = ""
    private val markerList: MutableList<AroCoordinates> = LinkedList()

    @Subscribe
    override fun onMessageEvent(messagePacket: MessagePacket) {
        when (messagePacket.type) {
            MatrixConnection.MARKER -> markerList.add(messagePacket.aroCoordinates)
            MatrixConnection.REMOVE_MARKER -> for (marker in markerList) {
                if (marker.equals(messagePacket.aroCoordinates)) markerList.remove(marker)
            }
        }
    }

    override fun iterator(): Iterator<AroCoordinates> {
        return markerList.iterator()
    }
}
