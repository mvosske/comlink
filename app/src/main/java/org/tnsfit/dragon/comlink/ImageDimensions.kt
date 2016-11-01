package org.tnsfit.dragon.comlink

import android.graphics.Matrix
import android.widget.ImageView

/**
 * Created by dragon on 01.11.16.
 * Data Class um reale Position und größe des angezeigten Bildes zu berechnen und zu speichern
 *
 */

class ImageDimensions(){

    var width: Int = 2
    var height: Int = 2
    var x: Int = 1
    var y: Int = 1
    var changePending: Boolean = false

    fun changeDimensions(imageView: ImageView) {
        val drawable = imageView.drawable ?: return
        val f = FloatArray(9)
        imageView.getImageMatrix().getValues(f)

        width = (drawable.intrinsicWidth * f[Matrix.MSCALE_X]).toInt()
        height = (drawable.intrinsicHeight * f[Matrix.MSCALE_Y]).toInt()

        x = ((imageView.width / 2) - (width / 2)).toInt()
        y = ((imageView.height / 2) - (height / 2)).toInt()

        /*
        val boardHalfWidth = (deviceSize.x / 2)
        val boardHalfHeight = (deviceSize.y / 2)
        val imageHalfWidth = (width / 2)
        val imageHalfHeight = (height / 2)

        x = boardHalfWidth - imageHalfWidth
        y = boardHalfHeight - imageHalfHeight
        */

    }
}
