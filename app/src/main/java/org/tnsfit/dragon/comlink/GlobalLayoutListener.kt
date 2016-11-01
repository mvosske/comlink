package org.tnsfit.dragon.comlink

import android.view.ViewTreeObserver
import android.widget.ImageView

/**
 * Created by dragon on 01.11.16.
 * Hack um die richtige größe des Displays zu bekommen
 *
 */

class GlobalLayoutListener(
        private val imageDimensions: ImageDimensions,
        private val aroManager: AroManager,
        private val imageView: ImageView
): ViewTreeObserver.OnGlobalLayoutListener {

    fun register() {
        imageView.viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    override fun onGlobalLayout() {
        if (!imageDimensions.changePending) return
        imageDimensions.changeDimensions(imageView)
        aroManager.rePlaceMarker()
        imageDimensions.changePending = false
    }
}
