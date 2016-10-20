package org.tnsfit.dragon.comlink.matrix

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationManagerCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.tnsfit.dragon.comlink.misc.AppConstants
import org.tnsfit.dragon.comlink.misc.registerIfRequired

/**
 * Created by dragon on 11.10.16.
 *
 *
 */


class MatrixService: Service(), ImageEventListener {

	companion object {
		fun start(context: Context) {
			context.startService(Intent(context, MatrixService::class.java))
		}
	}

	private val socketPool = SocketPool()
	private val mMatrix: MatrixConnection by lazy { MatrixConnection(socketPool,getExternalFilesDir(null)) }
	private val notificationManager: NotificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext) }
	private val notificationId = AppConstants.NOTIFICATION_ID_SERVICE_ALIVE

	private val eventBus = EventBus.getDefault()

	private val notificationActionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			intent.action?.let { action ->
				if (action == ServiceNotification.NOTIFICATION_ACTION_DISMISS) {
					this@MatrixService.stopSelf()
				}
			}
		}
	}

	override fun onCreate() {
		super.onCreate()
		this.notificationManager.notify(notificationId,
				ServiceNotification.buildNotificationServiceAlive(this.applicationContext).build())

		this.registerReceiver(this.notificationActionReceiver, ServiceNotification.filter)

		this.mMatrix.startServer()
		this.eventBus.registerIfRequired(this)
		this.eventBus.registerIfRequired(mMatrix)
	}

	override fun onDestroy() {
		socketPool.stop()

		this.unregisterReceiver(this.notificationActionReceiver)
		this.eventBus.unregister(this.mMatrix)
		this.eventBus.unregister(this)

		this.mMatrix.stop()
		socketPool.closeAllSockets()

		this.notificationManager.notify(notificationId, ServiceNotification.buildNotificationServiceNotAlive(this.applicationContext).build())
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
	override fun onBind(intent: Intent): IBinder? = null

	@Subscribe(threadMode = ThreadMode.ASYNC)
	override fun onImageEvent(imageUri: ImageEvent) {
		if (imageUri.source == MessagePacket.COMLINK) {
			SendAgent(socketPool, contentResolver.openInputStream(imageUri.image)).start()
		}
	}
}