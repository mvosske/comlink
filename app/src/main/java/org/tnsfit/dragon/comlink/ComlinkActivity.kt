package org.tnsfit.dragon.comlink

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
        StatusEventListener, KillEventListener, DownloadEventListener {

	private val eventBus = EventBus.getDefault()
    private val mSendTextListener = SendText(this)
    private val imageDimensions = ImageDimensions()
    private val aroManager = AroManager(imageDimensions)
    private val mFrame:RelativeLayout by lazy { findViewById(R.id.imageFrame) as RelativeLayout }
    private val mainImageView: ImageView by lazy { findViewById(R.id.main_image_view) as ImageView }

    private lateinit var statusTracker: StatusTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comlink)

        val retrievedTracker = eventBus.getStickyEvent(StatusTracker::class.java)
        if (retrievedTracker == null) {
            statusTracker = StatusTracker()
            eventBus.postSticky(statusTracker)
        } else {
            statusTracker = retrievedTracker
            setImage(statusTracker.currentHandout)
            (findViewById(R.id.name_display) as? TextView)?.text = statusTracker.name
            for (marker in statusTracker) {
                aroManager.placeMarker(this, mFrame, marker)
            }
        }

        findViewById(R.id.exit_button).setOnClickListener({ finish() })
        findViewById(R.id.send_Text).setOnClickListener(mSendTextListener)

        mSendTextListener.registerToggleButton(findViewById(R.id.button_main_toggle_input) as Button)
        mSendTextListener.registerEditor((findViewById(R.id.sendTextEdit) as EditText))

        AroPlacementListener(this.imageDimensions).listen(mainImageView)
        GlobalLayoutListener(imageDimensions,aroManager,mainImageView).register()
        MatrixService.start(this.applicationContext)
    }

    override fun onStart() {
        super.onStart()
		this.eventBus.registerIfRequired(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == AppConstants.INTENT_REQUEST_CONTENT) && (resultCode == Activity.RESULT_OK)) {
            val imageUri = data?.data ?: return
            val button = findViewById(R.id.button_main_send) as Button

            button.setOnClickListener({
                button.text = getText(R.string.information_main_sending_aborting)
                button.isEnabled = false
                button.setOnClickListener(null)
                statusTracker.lastEvent = StatusEvent(StatusTracker.STATUS_ABORTING)
            })
            eventBus.post(ImageEvent(imageUri,MessagePacket.COMLINK))
            button.text = "0" + getText(R.string.information_main_count_sent)
            setImage(imageUri)

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        imageDimensions.isChangePending = true
    }

    fun setImage(image: Uri) {
        // (findViewById(R.id.imageView) as ImageView).setImageURI(image)

        mainImageView.setImageBitmap(decodeUri(image))
        statusTracker.currentHandout = image
        imageDimensions.isChangePending = true
    }

    private fun decodeUri(selectedImage: Uri): Bitmap {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage), null, o)

        val REQUIRED_SIZE = 1000

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
    override fun onDownloadEvent(event: DownloadEvent) {
        if (event.source != MessagePacket.MATRIX) return
        DownloadDialog(this,event.address, event.destination).create().show()
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
            findViewById(R.id.button_main_send).isEnabled = false
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
        val button = findViewById(R.id.button_main_send) as Button
        val toggleButton = findViewById(R.id.button_main_toggle_input) as Button
        when (statusEvent.status) {
            StatusTracker.STATUS_PROGRESS -> {
                button.text = statusEvent.text + getText(R.string.information_main_count_sent)
            }

            StatusTracker.STATUS_ABORTING -> {
                button.text = getText(R.string.information_main_sending_aborting)
                button.isEnabled = false
                button.setOnClickListener(null)
            }

            StatusTracker.STATUS_IDLE -> {
                button.text = getText(R.string.label_main_send)
                button.isEnabled = true
                button.setOnClickListener(View.OnClickListener {
                        val i = Intent(Intent.ACTION_GET_CONTENT)
                        i.type = "*/*"
                        startActivityForResult(i, AppConstants.INTENT_REQUEST_CONTENT)
                    })
            }
        }
        statusTracker.lastEvent = statusEvent
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onKillEvent(event: KillEvent) {
        finish()
    }

    override fun onStop(){
        super.onStop()
		this.eventBus.unregister(this)
        this.eventBus.post(KillEvent(MessagePacket.COMLINK))
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
