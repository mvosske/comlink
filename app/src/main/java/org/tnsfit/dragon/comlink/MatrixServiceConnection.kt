package org.tnsfit.dragon.comlink

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder

/**
 * Created by dragon on 11.10.16.
 *
 */


class MatrixServiceConnection: ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {

    }

    override fun onServiceDisconnected(name: ComponentName?) {
        // local service, so this should never happen

    }

}
