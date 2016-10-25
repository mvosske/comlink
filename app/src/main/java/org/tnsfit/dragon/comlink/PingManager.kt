package org.tnsfit.dragon.comlink

import android.content.Context
import android.os.Handler
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessagePacket
import java.util.*

/**
 * Created by dragon on 11.10.16.
 *
 */


class PingManager {

    private val mHandler: Handler
    private val mPingList: MutableList<ImageView> = LinkedList<ImageView>()
    private val mMarkerList: MutableList<ImageView> = LinkedList<ImageView>()

    init {
        mHandler = Handler()
    }

    fun getPing (c: Context, frame: ViewGroup): ImageView {
        val result = get(c, mPingList)
        result.setImageResource(R.drawable.ping_image)
        result.setOnClickListener({ frame.removeView(result) })

        return result
    }

    fun getMarker (c: Context, frame: ViewGroup): ImageView {
        val result = get(c, mMarkerList)
        result.setImageResource(R.drawable.marker_image)
        result.setOnLongClickListener {
            EventBus.getDefault().post(
                    MessagePacket(MatrixConnection.MARKER,""))
            true
        }
        return result
    }

    private fun get(c: Context, list: MutableList<ImageView>): ImageView {
        for (v in list) {
            if (v.isAttachedToWindow) {
                continue
            } else {
                return v
            }
        }
        val result = ImageView(c)
        list.add(result)
        return result
    }

    private fun paramFromCoords(coordsString: String, frame: ViewGroup): RelativeLayout.LayoutParams {
        val coords = coordsString.split(",", limit = 2)
        val percentX: Int = coords[0].toInt()
        val percentY: Int = coords[1].toInt()

        val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
        try {
            val newX: Int = ((frame.width * percentX) / 100)
            val newY: Int = ((frame.height * percentY) / 100)
            params.topMargin = newY - 50
            params.leftMargin = newX - 50

        } catch (e: NumberFormatException) {
            // ToDo Report this Error;
        }
        return params
    }

    fun placePing(c: Context, frame: ViewGroup, coordsString: String) {
        val view = getPing(c, frame)
        val params = paramFromCoords(coordsString,frame)
        frame.addView(view, params)
        mHandler.postDelayed(Runnable { frame.removeView(view) }, 4000)
    }

    fun placeMarker (c: Context, frame: ViewGroup, coordsString: String) {
        val view = getMarker(c, frame)
        val params = paramFromCoords(coordsString,frame)
        frame.addView(view, params)
    }

}
