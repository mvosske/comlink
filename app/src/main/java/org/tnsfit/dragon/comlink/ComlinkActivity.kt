package org.tnsfit.dragon.comlink

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

class ComlinkActivity : Activity(), MessageEventListener, ImageEventListener,
        StatusEventListener, KillEventListener {

	private val eventBus = EventBus.getDefault()
    private val mSendTextListener = SendText(this)
    private val aroManager = AroManager()
    private val mFrame:RelativeLayout by lazy { findViewById(R.id.imageFrame) as RelativeLayout }

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

        aroManager.orientation = getResources().getConfiguration().orientation
        val retrievedTracker = eventBus.getStickyEvent(StatusTracker::class.java)
        if (retrievedTracker == null) {
            statusTracker = StatusTracker()
            eventBus.postSticky(statusTracker)
        } else {
            statusTracker = retrievedTracker
            setImage(statusTracker.currentHandout, true)
            (findViewById(R.id.name_display) as? TextView)?.text = statusTracker.name
            for (marker in statusTracker) {
                aroManager.placeMarker(this, mFrame, marker)
            }
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

        AroPlacementListener().listen(findViewById(R.id.imageFrame))

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

            button.setOnClickListener({ eventBus.post(StatusEvent(StatusTracker.STATUS_ABORTING)) })
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
        try {
            setImage(imageUri.image)
        } catch (e: Exception) {
            Toast.makeText(this,e.message,Toast.LENGTH_LONG).show()
        }

        if (statusTracker.lastEvent.status == StatusTracker.STATUS_IDLE)
            findViewById(R.id.send_image).isEnabled = false
    }

    fun setImage(image: Uri, oneShot: Boolean = false) {
        // (findViewById(R.id.imageView) as ImageView).setImageURI(image)
        (findViewById(R.id.imageView) as ImageView).setImageBitmap(decodeUri(image))
        if (!oneShot) statusTracker.currentHandout = image
    }

    private fun decodeUri(selectedImage: Uri): Bitmap {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o)

        val REQUIRED_SIZE = 100

        var width_tmp = o.outWidth
        var height_tmp = o.outHeight
        var scale = 1
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE) {
                break
            }
            width_tmp /= 2
            height_tmp /= 2
            scale *= 2
        }

        val o2 = BitmapFactory.Options()
        o2.inSampleSize = scale
        return BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o2)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onMessageEvent(messagePacket: MessagePacket) {
        if (messagePacket.source == MessagePacket.COMLINK) return
        val coords = AroCoordinates(messagePacket.message)
        when (messagePacket.type) {
            MatrixConnection.TEXT_MESSAGE -> Toast.makeText(this, messagePacket.message, Toast.LENGTH_SHORT).show()
            MatrixConnection.PING -> aroManager.placePing(this,mFrame,coords)
            MatrixConnection.MARKER -> aroManager.placeMarker(this,mFrame,coords)
            MatrixConnection.REMOVE_MARKER -> aroManager.removeMarker(mFrame, coords)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onStatusEvent(statusEvent: StatusEvent) {
        val button = findViewById(R.id.send_image) as Button
        val toggleButton = findViewById(R.id.toggle_text_input) as Button
        // ToDo die folgenden Strings als String Ressource
        when (statusEvent.status) {
            StatusTracker.STATUS_PROGRESS -> {
                button.text = statusEvent.text + " x gesendet"
            }

            StatusTracker.STATUS_ABORTING -> {
                button.text = "Warte auf Abbruch.."
                button.isEnabled = false
            }
            StatusTracker.STATUS_IDLE -> {
                button.text = "Send File"
                button.isEnabled = true
                button.setOnClickListener(sendListener)
                toggleButton.text = "Nachricht"
            }
        }
        statusTracker.lastEvent = statusEvent
    }

    override fun onKillEvent(event: KillEvent) {
        finish()
    }

    override fun onStop(){
        super.onStop()
		this.eventBus.unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
