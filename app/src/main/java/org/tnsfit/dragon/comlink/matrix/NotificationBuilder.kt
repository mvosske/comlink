package org.tnsfit.dragon.comlink.matrix

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.NotificationCompat
import org.tnsfit.dragon.comlink.R
import org.tnsfit.dragon.comlink.misc.AppConstants

/**
 * @author eric.neidhardt on 17.10.2016.
 */
object ServiceNotification {

	val NOTIFICATION_ACTION_DISMISS = "NOTIFICATION_ACTION_DISMISS"

	val filter: IntentFilter = IntentFilter().apply {
		this.addAction(NOTIFICATION_ACTION_DISMISS)
	}

	fun buildNotification(context: Context): NotificationCompat.Builder {

		val deleteIntent = PendingIntent.getBroadcast(context, AppConstants.INTENT_REQUEST_NOTIFICATION, Intent(NOTIFICATION_ACTION_DISMISS), 0)

		val builder = NotificationCompat.Builder(context)
				.setContentTitle("Titel")
				.setDeleteIntent(deleteIntent)
				.setSmallIcon(R.mipmap.ic_launcher)

		val wearableExtender = NotificationCompat.WearableExtender()
				.setHintHideIcon(true)
		// TODO set background icon

		builder.extend(wearableExtender)

		return builder
	}
}