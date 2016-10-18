package org.tnsfit.dragon.comlink.misc

import org.greenrobot.eventbus.EventBus

/**
 * @author eric.neidhardt on 18.10.2016.
 */
fun EventBus.registerIfRequired(listener: Any) {
	if (!this.isRegistered(listener))
		this.register(listener)
}