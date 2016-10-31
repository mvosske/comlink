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

class AroPlacementListener(): View.OnLongClickListener, View.OnTouchListener, View.OnClickListener {
    private val eventBus: EventBus by lazy {
        EventBus.getDefault()
    }

    private var mLastTouchX:Int=50
    private var mLastTouchY:Int=50

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if ((v == null) || (event == null)) return false

        mLastTouchX = (event.x*100/v.width).toInt()
        mLastTouchY = (event.y*100/v.height).toInt()

        return false
    }

    override fun onClick(v: View?) {
        eventBus.post(MessagePacket(
                MatrixConnection.PING,
                mLastTouchX.toString()+","+mLastTouchY.toString())
        )
    }

    override fun onLongClick(v: View?): Boolean {
        eventBus.post(MessagePacket(
                MatrixConnection.MARKER,
                mLastTouchX.toString()+","+mLastTouchY.toString())
        )
        return true
    }

    fun listen(v: View) {
        v.setOnTouchListener(this)
        v.setOnClickListener(this)
        v.setOnLongClickListener(this)
    }
}
