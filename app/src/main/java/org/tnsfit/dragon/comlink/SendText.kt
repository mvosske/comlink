package org.tnsfit.dragon.comlink

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessagePacket
import org.tnsfit.dragon.comlink.matrix.StatusEvent

/**
 * Created by dragon on 11.10.16.
 *
 */

class SendText(c: ComlinkActivity): TextView.OnEditorActionListener, View.OnClickListener {
    private var mMaster: ComlinkActivity
    private var mEditText: EditText? = null
    var mode = NAME

    companion object {
        val NAME = 0
        val TEXT = 1
    }

    init {
        mMaster = c
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (v == null || (v !is EditText)) return false
        if (event ?: 0 != KeyEvent.FLAG_EDITOR_ACTION) return false

        send()
        hideGroup(v.parent as? ViewGroup)
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.send_Text -> {
                send()
                hideGroup(v?.parent as? ViewGroup)
            }
        }
    }

    private fun send() {
        val textField = mEditText?.text ?: return
        val eventBus = EventBus.getDefault()
        val statusTracker = eventBus.getStickyEvent(StatusTracker::class.java)
        if (mode == NAME) {
            statusTracker.name = textField.toString()
            (mMaster.findViewById(R.id.name_display) as? TextView)?.setText(statusTracker.name)
            eventBus.post(StatusEvent(StatusTracker.STATUS_IDLE))
        } else {
            val message = statusTracker.name + ": \"" + textField + "\""
            eventBus.post(MessagePacket(MatrixConnection.TEXT_MESSAGE, message, MessagePacket.COMLINK))
        }
        textField.clear()
    }

    private fun hideGroup(group: ViewGroup?) {
        val view = mMaster.currentFocus
        if (view != null) {
            val imm = mMaster.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        group?.visibility = View.GONE
    }

    fun registerEditor(editText: EditText) {
        editText.setOnEditorActionListener(this)
        mEditText = editText
    }
}