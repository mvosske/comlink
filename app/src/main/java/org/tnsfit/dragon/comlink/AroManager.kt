package org.tnsfit.dragon.comlink

import android.content.Context
import android.os.Handler
import android.view.ViewGroup
import android.widget.ImageView
import java.util.*

/**
 * Created by dragon on 11.10.16.
 * Pings sind kurzeitiger Anzeiger
 * Marker sind Dauerhafte Anzeiger
 * Pings und Marker sind zusammen AROs
 */


class AroManager {

    private val mHandler: Handler
    private val mPingList: MutableList<ImageView> = LinkedList()
    private val mMarkerList: MutableList<ImageView> = LinkedList()
    private val deleteListener: AroDeleter = AroDeleter()

    init {
        mHandler = Handler()
    }

    fun getPing (c: Context): ImageView {
        val result = get(c, mPingList)
        result.setImageResource(R.drawable.ping_image)
        result.setOnClickListener(deleteListener)

        return result
    }

    fun getMarker (c: Context): ImageView {
        val result = get(c, mMarkerList)
        result.setImageResource(R.drawable.marker_image)
        result.setOnLongClickListener(deleteListener)
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

    fun placePing(c: Context, frame: ViewGroup, coords: AroCoordinates) {
        val view = getPing(c)
        frame.addView(view, coords.layoutParams(frame))
        mHandler.postDelayed(deleteListener.OnTime(view), 4000)
    }

    fun placeMarker (c: Context, frame: ViewGroup, coords: AroCoordinates) {
        val view = getMarker(c)
        view.tag = coords
        frame.addView(view, coords.layoutParams(frame))
    }

    fun removeMarker(frame: ViewGroup, coords: AroCoordinates) {
        for (i in 0..frame.childCount-1) {
            val child = frame.getChildAt(i)
            if (child.tag.equals(coords)) {
                frame.removeViewAt(i)
                child.tag = null
            }
        }
    }

}
