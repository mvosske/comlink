package org.tnsfit.dragon.comlink.matrix

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationManagerCompat
import org.greenrobot.eventbus.EventBus
import org.tnsfit.dragon.comlink.misc.AppConstants
import org.tnsfit.dragon.comlink.misc.registerIfRequired
import java.net.Socket
import java.util.*

/**
 * Created by dragon on 11.10.16.
 *
 *
 */


class MatrixService: Service() {

	companion object {
		fun start(context: Context) {
			context.startService(Intent(context, MatrixService::class.java))
		}
	}

	private val mSockets = ArrayList<Socket>()
	private val mMatrix = MatrixConnection(this)
	private val notificationManager: NotificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext) }
	private val notificationId = AppConstants.NOTIFICATION_ID_SERVICE_ALIVE

	private val eventBus = EventBus.getDefault()

	private val notificationActionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			intent.action?.let { action ->
				if (action == ServiceNotification.NOTIFICATION_ACTION_DISMISS) {
					this@MatrixService.notificationManager.cancel(notificationId)
					this@MatrixService.stopSelf()
				}
			}
		}
	}

	private var isRunning = false

	override fun onCreate() {
		super.onCreate()
		this.notificationManager.notify(notificationId,
				ServiceNotification.buildNotificationServiceAlive(this.applicationContext).build())

		this.registerReceiver(this.notificationActionReceiver, ServiceNotification.filter)

		this.isRunning = true

		this.mMatrix.startServer()
		this.eventBus.registerIfRequired(mMatrix)
	}

	override fun onDestroy() {
		this.unregisterReceiver(this.notificationActionReceiver)
		this.eventBus.unregister(this.mMatrix)
		this.isRunning = false
		this.mMatrix.stop()
		this.closeAllSockets()
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
	override fun onBind(intent: Intent): IBinder? = null

	fun registerSocket(socket: Socket): Boolean {
		synchronized(this) {
			if (!isRunning) return false
			mSockets.add(socket)
			return true
		}
	}

	fun unregisterSocket(socket: Socket) {
		synchronized(this) {
			if (!isRunning) return
			mSockets.remove(socket)
		}
	}

	private fun closeAllSockets() {
		synchronized(this) {
			for (socket in mSockets) {
				socket.close()
			}
		}
	}

}