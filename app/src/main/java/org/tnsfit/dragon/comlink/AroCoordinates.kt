package org.tnsfit.dragon.comlink

import android.view.ViewGroup
import android.widget.RelativeLayout

/**
 * Created by dragon on 26.10.16.
 * Enthält und speichert die Koordinaten für Pings und Marker (AROs)
 *
 */

class AroCoordinates(val x: Int, val y: Int) {
    fun layoutParams(dimension: ImageDimensions): ViewGroup.LayoutParams {
        val result = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)

        // hier kann eine Number Format Exception kommen, wäre aber ein nicht-Fangbarer Laufzeitfehler
        val newX: Int = ((dimension.width * x) / 100) + dimension.x
        val newY: Int = ((dimension.height * y) / 100)+ dimension.y

        result.topMargin = newY - 50
        result.leftMargin = newX - 50

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
    } catch (nfe: NumberFormatException) {
        // Auch hier die Standardwerte benutzen und alles wird gut
    }

    return AroCoordinates(x,y)
}