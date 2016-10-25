package org.tnsfit.dragon.comlink

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.tnsfit.dragon.comlink.matrix.*
import org.tnsfit.dragon.comlink.misc.AppConstants
import org.tnsfit.dragon.comlink.misc.registerIfRequired

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

        val retrievedTracker = eventBus.getStickyEvent(StatusTracker::class.java)
        if (retrievedTracker == null) {
            statusTracker = StatusTracker()
            eventBus.postSticky(statusTracker)
        } else {
            statusTracker = retrievedTracker
            setImage(statusTracker.currentHandout, true)
        }

        setContentView(R.layout.activity_comlink)

        findViewById(R.id.exit_button).setOnClickListener({ finish() })
        findViewById(R.id.send_image).setOnClickListener(sendListener)
        findViewById(R.id.toggle_text_input).setOnClickListener {
            findViewById(R.id.send_text_controls)?.visibility = View.VISIBLE
            val image: Drawable
            if (statusTracker.name == "") {
                image = getDrawable(android.R.drawable.ic_menu_save)
                mSendTextListener.mode = SendText.NAME
            } else {
                image = getDrawable(android.R.drawable.ic_menu_share)
                mSendTextListener.mode = SendText.TEXT
            }
            (findViewById(R.id.send_Text) as ImageButton).setImageDrawable(image)
        }

        findViewById(R.id.send_Text).setOnClickListener(mSendTextListener)
        mSendTextListener.registerEditor((findViewById(R.id.sendTextEdit) as EditText))

        PingListener().listen(findViewById(R.id.imageFrame))

		MatrixService.start(this.applicationContext)
    }

    override fun onStart() {
        super.onStart()
		this.eventBus.registerIfRequired(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == AppConstants.INTENT_REQUEST_CONTENT) && (resultCode == Activity.RESULT_OK)) {
            val imageUri = data?.data ?: return
            val button = findViewById(R.id.send_image) as Button

            button.setOnClickListener(View.OnClickListener { eventBus.post(StatusEvent(StatusTracker.ABORTING)) })
            eventBus.post(ImageEvent(imageUri,MessagePacket.COMLINK))
            button.text = "0 x gesendet"
            setImage(imageUri)

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onImageEvent(imageUri: ImageEvent) {
        if (imageUri.source == MessagePacket.COMLINK) return
        setImage(imageUri.image)
        if (statusTracker.lastEvent.status == StatusTracker.IDLE)
            findViewById(R.id.send_image).isEnabled = false
    }

    fun setImage(image: Uri, oneShot: Boolean = false) {
        (findViewById(R.id.imageView) as ImageView).setImageURI(image)
        if (!oneShot) statusTracker.currentHandout = image
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onMessageEvent(messagePacket: MessagePacket) {
        if (messagePacket.source == MessagePacket.COMLINK) return
        val frame = findViewById(R.id.imageFrame) as RelativeLayout
        when (messagePacket.type) {
            MatrixConnection.TEXT_MESSAGE -> Toast.makeText(this, messagePacket.message, Toast.LENGTH_SHORT).show()
            MatrixConnection.PING -> mPingManager.placePing(this,frame,messagePacket.message)
            MatrixConnection.MARKER -> mPingManager.placeMarker(this,frame,messagePacket.message)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onStatusEvent(statusEvent: StatusEvent) {
        val button = findViewById(R.id.send_image) as Button
        val toggleButton = findViewById(R.id.toggle_text_input) as Button
        // ToDo die folgenden Strings als String Ressource
        when (statusEvent.status) {
            StatusTracker.PROGRESS -> {
                button.text = statusEvent.text + " x gesendet"
            }

            StatusTracker.ABORTING -> {
                button.text = "Warte auf Abbruch.."
                button.isEnabled = false
            }
            StatusTracker.IDLE -> {
                button.text = "Send File"
                button.isEnabled = true
                button.setOnClickListener(sendListener)
                toggleButton.text = "Nachricht"
            }
        }
        statusTracker.lastEvent = statusEvent
    }

    override fun onStop(){
        super.onStop()
		this.eventBus.unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
