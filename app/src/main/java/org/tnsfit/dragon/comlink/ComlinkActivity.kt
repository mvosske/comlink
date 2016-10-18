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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class ComlinkActivity : Activity(), MessageEventListener, ImageEventListener {

	private val eventBus = EventBus.getDefault()

    private val mSendTextListener = SendText(this)
    private val mPingManager = PingManager()

    private var mSendAgent: SendAgentAsyncTask? = null

    // ToDo let the Service track this
    private var mCurrentHandoutURI = ""

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

		MatrixService.start(Application@this)

        mCurrentHandoutURI = savedInstanceState?.getString("HandoutURI") ?: ""
        if (mCurrentHandoutURI != "") setImage(Uri.parse(mCurrentHandoutURI))
    }

    override fun onStart() {
        super.onStart()
		this.eventBus.registerIfRequired(this)
    }

    fun readBytes(inputStream: InputStream): ByteArray {
        val byteBuffer = ByteArrayOutputStream()
        try {

            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)

            var len: Int
            while (true) {
                len = inputStream.read(buffer)
                if (len == -1) break
                byteBuffer.write(buffer, 0, len)
            }
        } catch (e: IOException) {
            return ByteArray(0)
        }

        return byteBuffer.toByteArray()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if ((requestCode == AppConstants.INTENT_REQUEST_CONTENT) && (resultCode == Activity.RESULT_OK)) {
            val imageUri = data?.data ?: return
            val button = findViewById(R.id.send_image) as Button

            mSendAgent = mSendAgent?.recycle() ?: SendAgentAsyncTask(button)
            mSendAgent?.execute(readBytes(contentResolver.openInputStream(imageUri)))
            // ToDo move AsyncTask to Service as pure Thread
            eventBus.post(MessagePacket(MatrixConnection.SEND,"handout"))
            setImage(imageUri)

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun placePing(coordsString: String) {
        val coords = coordsString.split(",", limit = 2)
        val percentX: Int = coords[0].toInt()
        val percentY: Int = coords[1].toInt()

        val frame = findViewById(R.id.imageFrame) as RelativeLayout
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
            // ToDo Toast this shit or so
            Log.e("ComlinkActivity", "Got Wrong format for a Ping")
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onImageEvent(eventUri: ImageEvent) {
        if (eventUri.source == MessagePacket.COMLINK) return
        setImage(eventUri.image)
    }

    fun setImage(image: Uri) {
        (findViewById(R.id.imageView) as ImageView).setImageURI(image)
        mCurrentHandoutURI = image.toString()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onMessageEvent(messagePacket: MessagePacket) {
        if (messagePacket.source == MessagePacket.COMLINK) return
        when (messagePacket.type) {
            MatrixConnection.TEXT_MESSAGE -> Toast.makeText(this, messagePacket.message, Toast.LENGTH_SHORT).show()
            MatrixConnection.PING -> placePing(messagePacket.message)
        }
    }

    fun sendAndHideTextField(message: String) {
        eventBus.post(MessagePacket(MatrixConnection.TEXT_MESSAGE,message))

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

	override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if (mCurrentHandoutURI != "") {
            outState?.putString("HandoutURI", mCurrentHandoutURI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSendAgent?.kill()
        mSendTextListener.kill()
    }
}
