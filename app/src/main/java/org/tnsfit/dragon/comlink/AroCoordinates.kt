package org.tnsfit.dragon.comlink

import android.content.res.Configuration
import android.view.ViewGroup
import android.widget.RelativeLayout

/**
 * Created by dragon on 26.10.16.
 * Enthält und speichert die Koordinaten für Pings und Marker (AROs)
 *
 */

class AroCoordinates(val x: Int, val y: Int) {
    fun layoutParams(frame: ViewGroup, orientation: Int): ViewGroup.LayoutParams {
        val result = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
        try {
            val newX: Int = ((frame.width * x) / 100)
            val newY: Int = ((frame.height * y) / 100)

            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                result.topMargin = newY - 50
                result.leftMargin = newX - 50
            } else { // Assert orientation == Landscape
                result.topMargin = newX - 50
                result.leftMargin = newY - 50
            }

        } catch (e: NumberFormatException) {
            // ToDo Report this Error;
        }
        return result
    }

    override fun hashCode(): Int {
        return x + (y*1000)
    }

    override fun equals(other: Any?): Boolean {
        if (other?.javaClass != AroCoordinates::class.java) return false
        return other?.hashCode() == hashCode()
    }

    override fun toString(): String {
        return x.toString() + "," + y.toString()
    }
}

fun AroCoordinates(coordsString: String): AroCoordinates {
    val coords = coordsString.split(",", limit = 2)

    var x: Int = 50
    var y: Int = 50

    try {
        x = coords[0].toInt()
        y = coords[1].toInt()
    } catch (ie: IndexOutOfBoundsException) {
        // Standarwerte wurden festgelegt, keine Behandlung notwendig
    }

    return AroCoordinates(x,y)
}