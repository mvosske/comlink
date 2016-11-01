package org.tnsfit.dragon.comlink

import android.view.MotionEvent
import android.view.View
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessagePacket

/**
 * Created by dragon on 08.10.16.
 *
 * Überwacht das Platzieren von Pings und Marker
 *
 */

class AroPlacementListener(val imageDimension: ImageDimensions): View.OnLongClickListener, View.OnTouchListener, View.OnClickListener {
    private val eventBus: EventBus by lazy {
        EventBus.getDefault()
    }

    private var mLastTouchX:Int=50
    private var mLastTouchY:Int=50

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if ((v == null) || (event == null)) return false

        // Pixel-Koordinaten auf dem Bild ausrechen
        val realX = event.x -imageDimension.x
        val realY = event.y -imageDimension.y

        // Prozentuale Position auf dem Bild ausrechnen
        mLastTouchX = (realX*100/imageDimension.width).toInt()
        mLastTouchY = (realY*100/imageDimension.height).toInt()

        return false
    }

    private fun coordString(): String {
        return mLastTouchX.toString()+","+mLastTouchY.toString()
    }

    override fun onClick(v: View?) {
        eventBus.post(MessagePacket(MatrixConnection.PING,coordString()))
    }

    override fun onLongClick(v: View?): Boolean {
        eventBus.post(MessagePacket(MatrixConnection.MARKER, coordString()))
        return true
    }

    fun listen(v: View) {
        v.setOnTouchListener(this)
        v.setOnClickListener(this)
        v.setOnLongClickListener(this)
    }
}
