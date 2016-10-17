package org.tnsfit.dragon.comlink.matrix

/**
 * @author eric.neidhardt on 17.10.2016.
 */

data class ServiceStartedEvent(val arbitraryData: String)

/**
 * Not actually required to define interface, but it helps to get a clear view.
 */
interface MatrixEventListener {
	fun onServiceStartedEvent(event: ServiceStartedEvent)
}