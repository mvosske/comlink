package org.tnsfit.dragon.comlink.matrix

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationManagerCompat
import org.tnsfit.dragon.comlink.misc.AppConstants

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

	private val notificationActionReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			intent.action?.let { action ->
				if (action == ServiceNotification.NOTIFICATION_ACTION_DISMISS)
					this@MatrixService.stopSelf()
			}
		}
	}

	private val notificationManager: NotificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext) }

	override fun onCreate() {
		super.onCreate()
		this.notificationManager.notify(AppConstants.NOTIFICATION_ID_SERVICE_ALIVE,
				ServiceNotification.buildNotification(this.applicationContext).build())

		this.registerReceiver(this.notificationActionReceiver, ServiceNotification.filter)
	}

	override fun onDestroy() {
		// cleanup
		this.unregisterReceiver(this.notificationActionReceiver)
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

	override fun onBind(intent: Intent): IBinder? = null
}