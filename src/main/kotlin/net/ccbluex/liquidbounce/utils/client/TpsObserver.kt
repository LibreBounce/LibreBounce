/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket

object TpsObserver : EventListener {

    // defines how many packets are recorded to get the average
    private const val AVERAGE_OF = 15

    // stores last intervals between WorldTimeUpdateS2CPackets
    private val intervals = ArrayDeque<Double>(AVERAGE_OF + 1)

    private val chronometer = Chronometer()
    private var wasDisconnected = true
    var tps = Double.NaN

    @Suppress("unused")
    private val packetHandler =  handler<PacketEvent> { event ->
        val packet = event.packet

        // the world time update packet should be sent once every second
        if (packet !is WorldTimeUpdateS2CPacket) {
            return@handler
        }

        if (wasDisconnected && intervals.isEmpty()) {
            wasDisconnected = false
            chronometer.reset()
            return@handler
        }

        val currentTime = System.currentTimeMillis()
        val elapsed = chronometer.elapsedUntil(currentTime).toDouble()
        chronometer.reset(currentTime)

        intervals.addLast(elapsed)
        while (intervals.size > AVERAGE_OF) {
            intervals.removeFirst()
        }

        val averageInterval = intervals.average()
        mc.renderTaskQueue.add(Runnable {
            tps = if (averageInterval > 0 && !averageInterval.isNaN()) {
                (20.0 / (averageInterval / 1000.0)).coerceIn(0.0..20.0)
            } else {
                Double.NaN
            }
        })
    }

    @Suppress("unused")
    private val disconnectHandler =  handler<DisconnectEvent> { _ ->
        wasDisconnected = true
        intervals.clear()
        tps = Double.NaN
    }

}
