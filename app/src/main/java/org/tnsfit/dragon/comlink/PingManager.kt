package org.tnsfit.dragon.comlink

import android.content.Context
import android.os.Handler
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import java.util.*

/**
 * Created by dragon on 11.10.16.
 *
 */


class PingManager {

    private val mHandler: Handler
    private val mPingList = LinkedList<ImageView>()

    init {
        mHandler = Handler()
    }

    fun get(c: Context, frame: ViewGroup): ImageView {
        for (v in mPingList) {
            if (v.isAttachedToWindow) {
                continue
            } else {
                return v
            }
        }
        val result = ImageView(c)
        result.setImageResource(R.drawable.ping_image)
        result.setOnClickListener { frame.removeView(result) }
        mPingList.add(result)
        return result
    }

    fun place(c: Context, frame: ViewGroup, params: ViewGroup.LayoutParams) {
        val view = get(c, frame)
        frame.addView(view, params)
        mHandler.postDelayed(Runnable { frame.removeView(view) }, 4000)
    }

}
