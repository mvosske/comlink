package org.tnsfit.dragon.comlink

import android.view.MotionEvent
import android.view.View

/**
 * Created by dragon on 08.10.16.
 *
 */

class PingListener(matrix: Matrix): View.OnLongClickListener, View.OnTouchListener {

    private val mMatrix: Matrix
    init{
        mMatrix = matrix
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
        mMatrix.ping(mLastTouchX.toString()+","+mLastTouchY.toString())
        return true
    }
}
