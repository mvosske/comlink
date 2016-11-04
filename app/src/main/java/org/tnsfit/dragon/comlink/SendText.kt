package org.tnsfit.dragon.comlink

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.matrix.MatrixConnection
import org.tnsfit.dragon.comlink.matrix.MessagePacket
import org.tnsfit.dragon.comlink.matrix.StatusEvent
import org.tnsfit.dragon.comlink.misc.AppConstants

/**
 * Created by dragon on 11.10.16.
 * Listener um Textnachrichten zu verschicken und den Namen einzugeben und zu speichern
 *
 */

class SendText(val mMaster: ComlinkActivity): TextView.OnEditorActionListener, View.OnClickListener {

    val eventBus: EventBus by lazy { EventBus.getDefault() }
    private val viewGroup: View by lazy { mMaster.findViewById(R.id.send_text_controls) }

    private var mEditText: EditText? = null
    var mode = NAME

    companion object {
        val NAME = R.string.label_main_name
        val MESSAGE = R.string.label_main_message
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (v == null || (v !is EditText)) return false
        val flagCheck = (event?.action ?: 0) and KeyEvent.FLAG_EDITOR_ACTION
        if (flagCheck != KeyEvent.FLAG_EDITOR_ACTION) return false

        submit()
        return true
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.send_Text -> {
                submit()
            }

            R.id.button_main_toggle_input -> {
                if ((mMaster.findViewById(R.id.name_display) as? TextView)?.text ?: "" == "") {
                    applyMode(NAME)
                } else {
                    applyMode(MESSAGE)
                }
                viewGroup.visibility = View.VISIBLE
            }
        }
    }

    private fun submit() {
        val textInput = mEditText?.text ?: return
        val statusTracker = eventBus.getStickyEvent(StatusTracker::class.java)

        if (mode == NAME) {
            statusTracker.name = textInput.toString()
            if (statusTracker.name != "") saveName(statusTracker.name)
        } else {
            val message = statusTracker.name + ": \"" + textInput + "\""
            eventBus.post(MessagePacket(MatrixConnection.TEXT_MESSAGE, message, MessagePacket.COMLINK))
        }

        textInput.clear()

        // die Gruppe mit den Eingabekontrollen wieder verstecken
        val focusView = mMaster.currentFocus
        if (focusView != null) {
            val imm = mMaster.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(focusView.windowToken, 0)
        }
        viewGroup.visibility = View.GONE

    }

    fun saveName(name: String) {

        applyMode(MESSAGE)
        (mMaster.findViewById(R.id.name_display) as? TextView)?.text = name

        val optionsEditor = mMaster.getPreferences(Context.MODE_PRIVATE).edit()
        optionsEditor.putString(AppConstants.OPTION_NAME, name).apply()
    }

    fun applyMode(newMode: Int = mode) {
        mode = newMode
        applyMode()
    }
    fun applyMode() {

        (mMaster.findViewById(R.id.button_main_toggle_input) as? Button)?.text = mMaster.getText(mode)
        val image: Drawable

        when (mode) {
            NAME -> {
                image = mMaster.getDrawable(android.R.drawable.ic_menu_save)
                viewGroup.visibility = View.VISIBLE
                // eventBus.post(StatusEvent(StatusTracker.STATUS_BLOCKED))
            }
            MESSAGE -> {
                image = mMaster.getDrawable(android.R.drawable.ic_menu_share)

                val tracker = eventBus.getStickyEvent(StatusTracker::class.java)
                if (tracker.lastEvent.status == StatusTracker.STATUS_BLOCKED)
                    eventBus.post(StatusEvent(StatusTracker.STATUS_IDLE))
            }
            else -> image = mMaster.getDrawable(R.drawable.empty_image)
        }
        (mMaster.findViewById(R.id.send_Text) as ImageButton).setImageDrawable(image)
    }

    fun registerEditor(editText: EditText, startValue: String = "") {
        editText.setOnEditorActionListener(this)
        mEditText = editText
        mEditText?.setText(startValue)
    }

    fun registerToggleButton(button: Button) {
        button.setOnClickListener(this)
        button.text = mMaster.getText(mode)
        this.onClick(null)
    }
}