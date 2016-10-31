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


class MatrixService: Service(), ImageEventListener, KillEventListener, DownloadEventListener {

	companion object {
		fun start(context: Context) {
			context.startService(Intent(context, MatrixService::class.java))
		}
	}

	private val socketPool = SocketPool()
	private val mMatrix: MatrixConnection by lazy { MatrixConnection() }
	private val notificationManager: NotificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext) }
	private val notificationId = AppConstants.NOTIFICATION_ID_SERVICE_ALIVE

	private val eventBus = EventBus.getDefault()

	private val notificationActionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			intent.action?.let { action ->
				if (action == ServiceNotification.NOTIFICATION_ACTION_DISMISS) {
					eventBus.post(KillEvent(MessagePacket.MATRIX))
					this@MatrixService.eventBus.removeAllStickyEvents()
					this@MatrixService.stopSelf()
				}
			}
		}
	}

	override fun onCreate() {
		super.onCreate()
		//this.notificationManager.notify(notificationId,
		//		ServiceNotification.buildNotificationServiceAlive(this.applicationContext).build())

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

		this.notificationManager.cancel(AppConstants.NOTIFICATION_ID_SERVICE_ALIVE)
		// this.notificationManager.notify(notificationId, ServiceNotification.buildNotificationServiceNotAlive(this.applicationContext).build())
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startForeground(notificationId,
				ServiceNotification.buildNotificationServiceAlive(this.applicationContext).build())
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent): IBinder? = null

	@Subscribe
	override fun onKillEvent(event: KillEvent) {
		if (event.source == MessagePacket.COMLINK) stopForeground(false)
	}

	@Subscribe(threadMode = ThreadMode.BACKGROUND)
	override fun onDownloadEvent(event: DownloadEvent) {
		if (event.source == MessagePacket.COMLINK) {
			RetrieveAgent(socketPool, event, getExternalFilesDir(null)).start()
		}
	}

	@Subscribe(threadMode = ThreadMode.BACKGROUND)
	override fun onImageEvent(imageUri: ImageEvent) {
		if (imageUri.source == MessagePacket.COMLINK) {
			SendAgent(socketPool, contentResolver.openInputStream(imageUri.image)).start()
		}
	}
}
