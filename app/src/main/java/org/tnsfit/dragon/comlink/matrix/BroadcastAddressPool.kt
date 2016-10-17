package org.tnsfit.dragon.comlink.matrix

import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.util.*

/**
 * Created by dragon on 14.10.16.
 *
 */

class BroadcastAddressPool {

    val mAllAddresses = LinkedList<InetAddress>()
    // assert that only one net at the same time is applicable
    var mConfirmedAddress: InetAddress? = null

    init {
        for (card in NetworkInterface.getNetworkInterfaces()) {
            if (!card.isUp || card.isLoopback) continue
            for (address in card.interfaceAddresses) {
                val bcast = address.getBroadcast()
                if (bcast != null) mAllAddresses.add(bcast)
            }
        }
    }


    fun isNetworkOf(testIp: InetAddress, netInterface: InterfaceAddress): Boolean {
        val ip = testIp.address
        val bcast: ByteArray = netInterface.broadcast?.address ?: return false
        if (bcast.size != ip.size) return false

        var prefix = netInterface.networkPrefixLength.toInt()
        val netmask = LinkedList<Int>()

        while (prefix > 0) {
            val maskBlockPrefix: Int = if (prefix >= 8) 8 else prefix.toInt()
            val maskBlock = (0xFF shl 8-maskBlockPrefix).toInt()
            netmask.add(maskBlock)
            prefix -= 8
        }

        var difference = 0
        var mask: Int

        for (i in 0..bcast.size-1) {
            val intIP = ip[i].toInt()
            val intBcast = bcast[i].toInt()
            try {
                mask = netmask.get(i)
            } catch (e: IndexOutOfBoundsException) {
                mask=0
            }

            difference += ((intIP and mask) xor (intBcast and mask))
        }

        return (difference == 0)
    }


    fun getPackets(message:ByteArray): List<DatagramPacket> {
        val result = LinkedList<DatagramPacket>()

        if (mConfirmedAddress != null) {
            result.add(DatagramPacket(message, message.size, mConfirmedAddress, 24322))
            return result
        }

        for (address in mAllAddresses) {
            result.add(DatagramPacket(message, message.size, address, 24322))
        }

        return result
    }

    fun confirm(ip: InetAddress) {
        if (mConfirmedAddress != null) return
        if (!ip.equals(mConfirmedAddress)) mConfirmedAddress = ip

        for (card in NetworkInterface.getNetworkInterfaces()) {
            if (!card.isUp || card.isLoopback) continue
            for (address in card.interfaceAddresses) {
                if (isNetworkOf(ip,address)) {
                    mConfirmedAddress = address.broadcast
                    return
                }
            }
        }
    }
}