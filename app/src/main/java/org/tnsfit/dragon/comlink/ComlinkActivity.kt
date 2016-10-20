package org.tnsfit.dragon.comlink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.tnsfit.dragon.comlink.matrix.*
import org.tnsfit.dragon.comlink.misc.AppConstants
import org.tnsfit.dragon.comlink.misc.registerIfRequired
import java.io.File

class ComlinkActivity : Activity(), MessageEventListener, ImageEventListener, StatusEventListener {

	private val eventBus = EventBus.getDefault()
    private val mSendTextListener = SendText(this)
    private val mPingManager = PingManager()

    private lateinit var statusTracker: StatusTracker

    val sendListener: View.OnClickListener by lazy {
        View.OnClickListener {
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.type = "*/*"
            startActivityForResult(i, AppConstants.INTENT_REQUEST_CONTENT)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comlink)

        findViewById(R.id.exit_button).setOnClickListener({ finish() })
        findViewById(R.id.send_image).setOnClickListener(sendListener)
        findViewById(R.id.send_text).setOnClickListener({ findViewById(R.id.send_text_controls).visibility = View.VISIBLE })

        findViewById(R.id.send_image).setOnLongClickListener {
            setImage(Uri.fromFile(File(getExternalFilesDir(null),"handout")))
            true // aka return true
        }

        val pingListener = PingListener()
        val textField = (findViewById(R.id.sendTextEdit) as EditText)

        textField.setOnEditorActionListener (mSendTextListener)
        findViewById(R.id.sendTextSend).setOnClickListener { sendAndHideTextField(textField.text.toString()) }
        findViewById(R.id.imageFrame).setOnTouchListener(pingListener)
        findViewById(R.id.imageFrame).setOnLongClickListener(pingListener)

		MatrixService.start(this.applicationContext)

        statusTracker = eventBus.getStickyEvent(StatusTracker::class.java) ?: StatusTracker()
        if (!statusTracker.isNew) setImage(statusTracker.currentHandout)
    }

    override fun onStart() {
        super.onStart()
		this.eventBus.registerIfRequired(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == AppConstants.INTENT_REQUEST_CONTENT) && (resultCode == Activity.RESULT_OK)) {
            val imageUri = data?.data ?: return
            val button = findViewById(R.id.send_image) as Button

            button.isEnabled = false
            button.setOnClickListener(View.OnClickListener { eventBus.post(StatusEvent(StatusTracker.ABORTING)) })
            eventBus.post(ImageEvent(imageUri,MessagePacket.COMLINK))
            setImage(imageUri)

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun placePing(coordsString: String) {
        val frame = findViewById(R.id.imageFrame) as RelativeLayout

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
            mPingManager.place(this,frame,params)

        } catch (e: NumberFormatException) {
            // ToDo Toast this error or so
            Log.e("ComlinkActivity", "Got Wrong format for a Ping")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onImageEvent(imageUri: ImageEvent) {
        if (imageUri.source == MessagePacket.COMLINK) return
        setImage(imageUri.image)
    }

    fun setImage(image: Uri) {
        (findViewById(R.id.imageView) as ImageView).setImageURI(image)
        statusTracker.currentHandout = image
        statusTracker.isNew = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onMessageEvent(messagePacket: MessagePacket) {
        if (messagePacket.source == MessagePacket.COMLINK) return
        when (messagePacket.type) {
            MatrixConnection.TEXT_MESSAGE -> Toast.makeText(this, messagePacket.message, Toast.LENGTH_SHORT).show()
            MatrixConnection.PING -> placePing(messagePacket.message)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onStatusEvent(statusEvent: StatusEvent) {
        val button = findViewById(R.id.send_image) as Button
        // ToDo die folgenden Strings als String Ressource
        when (statusEvent.status) {
            StatusTracker.PROGRESS,
            StatusTracker.SENDING -> {
                button.text = statusEvent.text + " x gesendet"
                button.isEnabled = true
            }

            StatusTracker.ABORTING -> {
                button.text = "Warte auf Abbruch.."
                button.isEnabled = false
            }
            StatusTracker.IDLE -> {
                button.text = "Send File"
                button.isEnabled = true
                button.setOnClickListener(sendListener)
            }
        }
        statusTracker.lastEvent = statusEvent
    }

    fun sendAndHideTextField(message: String) {
        eventBus.post(MessagePacket(MatrixConnection.TEXT_MESSAGE,message, MessagePacket.COMLINK))

        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
		}
        (findViewById(R.id.sendTextEdit) as EditText).text.clear()
        (findViewById(R.id.send_text_controls) as ViewGroup).visibility = View.GONE
    }

    override fun onStop(){
        super.onStop()
		this.eventBus.unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSendTextListener.kill()
    }
}
