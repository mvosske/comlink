package org.tnsfit.dragon.comlink

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by dragon on 11.10.16.
 *
 */

class SendText(c: ComlinkActivity): TextView.OnEditorActionListener {

    private var mMaster: ComlinkActivity?

    init {
        mMaster = c
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (event ?: 0 != KeyEvent.FLAG_EDITOR_ACTION) return false
        if (v == null) return false
        mMaster?.sendAndHideTextField(v.text.toString())
        return true
    }

    fun kill () {
        mMaster = null
    }
}