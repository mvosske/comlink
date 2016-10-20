package org.tnsfit.dragon.comlink

import android.net.Uri
import org.tnsfit.dragon.comlink.matrix.StatusEvent

/**
 * Created by dragon on 19.10.16.
 *
 * Verfolgt den Status des Senden Buttons, des angezeigten Handouts und der platzierten Marker
 * solange entweder der Service oder die Activity läuft
 *
 */

class StatusTracker {

    companion object {
        val IDLE = 0
        val SENDING = 1
        val PROGRESS = 2
        val ABORTING = 3
    }

    var isNew: Boolean = true
    var lastEvent: StatusEvent = StatusEvent(IDLE)
    var currentHandout: Uri = Uri.EMPTY

}
