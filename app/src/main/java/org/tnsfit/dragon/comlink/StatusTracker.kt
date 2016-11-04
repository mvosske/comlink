package org.tnsfit.dragon.comlink

import android.net.Uri
import org.greenrobot.eventbus.Subscribe
import org.tnsfit.dragon.comlink.matrix.*
import java.util.*

/**
 * Created by dragon on 19.10.16.
 *
 * Verfolgt den Status des Senden Buttons, des angezeigten Handouts und der platzierten Marker
 * solange entweder der Service oder die Activity läuft
 *
 */

class StatusTracker(): MessageEventListener, StatusEventListener, ImageEventListener,
        Iterable<AroCoordinates> {
    companion object {
        val STATUS_BLOCKED = 0
        val STATUS_IDLE = 1
        val STATUS_PROGRESS = 2
        val STATUS_ABORTING = 3

        val MODE_NULL = 10
        val MODE_CHUMMER = 11
        val MODE_JOHNSON = 12
    }

    var lastEvent: StatusEvent = StatusEvent(STATUS_BLOCKED)
    var currentHandout: Uri = Uri.parse("android.resource://org.tnsfit.dragon.comlink/drawable/empty_image")
    var name: String = ""
    var operationMode = MODE_NULL
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

    @Subscribe()
    override fun onStatusEvent(statusEvent: StatusEvent) {
        lastEvent = statusEvent
        if (statusEvent.status == STATUS_PROGRESS) operationMode = MODE_JOHNSON
    }

    @Subscribe
    override fun onImageEvent(imageUri: ImageEvent) {
        if (imageUri.source == MessagePacket.MATRIX) operationMode = MODE_CHUMMER
    }

    override fun iterator(): Iterator<AroCoordinates> {
        return markerList.iterator()
    }
}
