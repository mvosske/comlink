package org.tnsfit.dragon.comlink

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import org.tnsfit.dragon.comlink.matrix.MatrixService
import org.tnsfit.dragon.comlink.misc.AppConstants
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class ComlinklActivity : Activity() {

    private val mMatrix = Matrix(Handler(),this)
    private val mSocketManager = SocketManager()
    private val mSendTextListener = SendText(this)
    private val mPingManager = PingManager()

    private var mSendAgent: SendAgentAsyncTask? = null
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
        setContentView(R.layout.activity_comlinkl)

        findViewById(R.id.exit_button).setOnClickListener({ finish() })
        findViewById(R.id.send_image).setOnClickListener(sendListener)
        findViewById(R.id.send_text).setOnClickListener({ findViewById(R.id.send_text_controls).visibility = View.VISIBLE })

        findViewById(R.id.send_image).setOnLongClickListener {
            fillImage(File(getExternalFilesDir(null),"handout"))
            true // aka return true
        }

        val pingListener = PingListener(mMatrix)
        val textField = (findViewById(R.id.sendTextEdit) as EditText)

        textField.setOnEditorActionListener (mSendTextListener)
        findViewById(R.id.sendTextSend).setOnClickListener { sendAndHideTextField(textField.text.toString()) }
        findViewById(R.id.imageFrame).setOnTouchListener(pingListener)
        findViewById(R.id.imageFrame).setOnLongClickListener(pingListener)

        //val i = Intent(applicationContext, MatrixService::class.java )
        //bindService(i,MatrixServiceConnection(), Context.BIND_ADJUST_WITH_ACTIVITY)
		MatrixService.start(this)

        mCurrentHandoutURI = savedInstanceState?.getString("HandoutURI") ?: ""
        if (mCurrentHandoutURI != "") fillImage(Uri.parse(mCurrentHandoutURI))
    }

    override fun onStart() {
        super.onStart()
        mMatrix.startServer()
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
        if ((requestCode == 1) && (resultCode == Activity.RESULT_OK)) {
            val imageUri = data?.data ?: return
			val iv = findViewById(R.id.imageView) as ImageView
            val button = findViewById(R.id.send_image) as Button

            iv.setImageURI(imageUri)
            mCurrentHandoutURI = imageUri.toString()

            mSendAgent = mSendAgent?.recycle() ?: SendAgentAsyncTask(button,mSocketManager)
            mSendAgent?.execute(readBytes(contentResolver.openInputStream(imageUri)))
            mMatrix.send(Matrix.const.SEND,"handout")

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun ping(message: String) {
        val frame = findViewById(R.id.imageFrame) as RelativeLayout
        val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)

        try {
            val coords = message.split(",", limit = 2)
            val percentX: Int = coords[0].toInt()
            val percentY: Int = coords[1].toInt()

            val x: Int = ((frame.width * percentX) / 100)
            val y: Int = ((frame.height * percentY) / 100)

            params.topMargin = y - 50
            params.leftMargin = x - 50

            mPingManager.place(this,frame,params)

        } catch (e: NumberFormatException) {
            // ToDo Toast this shit or so
            Log.e("ComlinkActivity", "Got Wrong format for a Ping")
        }
    }

    fun fillImage (file: File) {
        if (file.isFile) {
            fillImage(Uri.fromFile(file))
            //(findViewById(R.id.imageView) as ImageView).setImageDrawable(Drawable.createFromPath(file.absolutePath))
        }
    }

    private fun fillImage(uriOfImage: Uri) {
        (findViewById(R.id.imageView) as ImageView).setImageURI(uriOfImage)
        mCurrentHandoutURI = uriOfImage.toString()
    }

    fun sendAndHideTextField(message: String) {
        mMatrix.send(Matrix.const.MESSAGE,message)

        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
		}
        (findViewById(R.id.sendTextEdit) as EditText).text.clear()
        (findViewById(R.id.send_text_controls) as ViewGroup).visibility = View.GONE
    }

    fun getSocketManager(): SocketManager {
        return mSocketManager
    }

    override fun onStop(){
        super.onStop()
        mMatrix.stop()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)

        if (mCurrentHandoutURI != "") {
            outState?.putString("HandoutURI", mCurrentHandoutURI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSendAgent?.cancel(true)
        mSendTextListener.kill()
    }
}
