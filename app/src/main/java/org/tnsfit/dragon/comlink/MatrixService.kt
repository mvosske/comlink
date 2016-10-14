package org.tnsfit.dragon.comlink

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Created by dragon on 11.10.16.
 *
 *
 */


class MatrixService: Service() {

    public inner class LocalBinder: Binder() {
        fun getService(): MatrixService {
            return this@MatrixService
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return LocalBinder()
    }
}