package org.tnsfit.dragon.comlink.matrix

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.support.v4.app.NotificationCompat
import org.tnsfit.dragon.comlink.ComlinkActivity
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

		val openAppIntent = PendingIntent.getActivity(context,
				AppConstants.INTENT_REQUEST_NOTIFICATION_OPEN, Intent(context, ComlinkActivity::class.java), 0)
		val deleteIntent = PendingIntent.getBroadcast(context,
				AppConstants.INTENT_REQUEST_NOTIFICATION_DISMISS, Intent(NOTIFICATION_ACTION_DISMISS), 0)

		val builder = NotificationCompat.Builder(context)
				.setContentTitle(context.getString(R.string.matrix_service_notification_title))
				.setDeleteIntent(deleteIntent)
				.setSmallIcon(R.drawable.ic_notification_matrix_on)
				.setContentIntent(openAppIntent)
				.setLargeIcon(this.getLargeIcon(context))
				.addAction(0, context.getString(R.string.matrix_service_notification_action_shutdown), deleteIntent)

		val wearableExtender = NotificationCompat.WearableExtender()
				.setHintHideIcon(true)

		builder.extend(wearableExtender)

		return builder
	}

	private fun getLargeIcon(context: Context): Bitmap {
		val resources = context.resources
		val requiredHeight = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
		val requiredWidth = resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)

		val size = getBitmapSize(resources, R.drawable.ic_notification_large)
		val sampleSize = getSampleFactor(size.x, size.y, requiredWidth, requiredHeight)
		return getBitmapFromAsset(context, R.drawable.ic_notification_large, sampleSize)
	}

	private fun getBitmapSize(resources: Resources, drawableId: Int): Point {
		val options = BitmapFactory.Options()
		options.inJustDecodeBounds = true
		BitmapFactory.decodeResource(resources, drawableId, options)
		val imageHeight = options.outHeight
		val imageWidth = options.outWidth
		return Point(imageWidth, imageHeight)
	}

	private fun getSampleFactor(width: Int, height: Int, requiredWidth: Int, requiredHeight: Int): Int {
		var inSampleSize = 1
		if (height > requiredHeight || width > requiredWidth) {
			val halfHeight = height / 2
			val halfWidth = width / 2

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) >= requiredHeight && (halfWidth / inSampleSize) >= requiredWidth)
				inSampleSize *= 2
		}
		return inSampleSize
	}

	private fun getBitmapFromAsset(context: Context, drawableId: Int, scaleFactor: Int): Bitmap
	{
		val options = BitmapFactory.Options()
		options.inSampleSize = scaleFactor
		options.inJustDecodeBounds = false
		return BitmapFactory.decodeResource(context.resources, drawableId, options)
	}
}