package org.tnsfit.dragon.comlink

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
        val NAME = R.string.label_main_name
        val MESSAGE = R.string.label_main_message
    }

    init {
        mMaster = c
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (v == null || (v !is EditText)) return false
        val flagCheck = (event?.action ?: 0) and KeyEvent.FLAG_EDITOR_ACTION
        if (flagCheck != KeyEvent.FLAG_EDITOR_ACTION) return false

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

            R.id.button_main_toggle_input -> {
                mMaster.findViewById(R.id.send_text_controls)?.visibility = View.VISIBLE
                val image: Drawable
                if ((mMaster.findViewById(R.id.name_display) as? TextView)?.text ?: "" == "") {
                    image = mMaster.getDrawable(android.R.drawable.ic_menu_save)
                    mode = SendText.NAME
                } else {
                    image = mMaster.getDrawable(android.R.drawable.ic_menu_share)
                    mode = SendText.MESSAGE
                }
                (mMaster.findViewById(R.id.send_Text) as ImageButton).setImageDrawable(image)
            }
        }
    }

    private fun send() {
        val textInput = mEditText?.text ?: return
        val toggleButton = (mMaster.findViewById(R.id.button_main_toggle_input) as? Button)
        val eventBus = EventBus.getDefault()
        val statusTracker = eventBus.getStickyEvent(StatusTracker::class.java)
        if (mode == NAME) {
            statusTracker.name = textInput.toString()
            if (statusTracker.name != "") {
                (mMaster.findViewById(R.id.name_display) as? TextView)?.text = statusTracker.name
                mode = MESSAGE
                toggleButton?.text = mMaster.getText(MESSAGE)
                eventBus.post(StatusEvent(StatusTracker.STATUS_IDLE)) // war BLOCKED
            }
        } else {
            val message = statusTracker.name + ": \"" + textInput + "\""
            eventBus.post(MessagePacket(MatrixConnection.TEXT_MESSAGE, message, MessagePacket.COMLINK))
        }
        textInput.clear()
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

    fun registerToggleButton(button: Button) {
        button.setOnClickListener(this)
        button.text = mMaster.getText(mode)
    }
}