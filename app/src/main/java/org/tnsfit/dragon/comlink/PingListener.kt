package org.tnsfit.dragon.comlink

import android.view.MotionEvent
import android.view.View
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessagePacket
import org.tnsfit.dragon.comlink.matrix.PingEvent

/**
 * Created by dragon on 08.10.16.
 *
 */

class PingListener(): View.OnLongClickListener, View.OnTouchListener {

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

    override fun onLongClick(v: View?): Boolean {
        eventBus.post(PingEvent(mLastTouchX,mLastTouchY))

        val coordString = mLastTouchX.toString()+","+mLastTouchY.toString()
        eventBus.post(MessagePacket(MatrixConnection.PING,coordString))
        return true
    }
}
