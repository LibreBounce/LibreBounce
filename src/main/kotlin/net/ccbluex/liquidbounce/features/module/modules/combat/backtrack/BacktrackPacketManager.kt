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
package net.ccbluex.liquidbounce.features.module.modules.combat.backtrack

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.arePacketQueuesEmpty
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.clear
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.currentDelay
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.delay
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.packetProcessQueue
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.processPackets
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.shouldCancelPackets
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack.shouldImmediatelyClear
import net.ccbluex.liquidbounce.interfaces.GlobalEntityAddition
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.TrackedPosition
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.util.math.Vec3d

/**
 * Backtrack's own packet manager. It is meant to be replaced by [PacketQueueManager]
 * but once the packet process logic is fixed.
 */
object BacktrackPacketManager : EventListener {

    /**
     * This manages every living entity's actual unfiltered position, allowing Backtrack to display them properly
     * so the transitions to the target's server position are done seamlessly.
     *
     * [GlobalEntityAddition] can also be used to create a ForwardTrack module.
     */
    val positionUpdateHandler = handler<PacketEvent>(priority = CRITICAL_MODIFICATION) { event ->
        val world = mc.world ?: return@handler

        when (val packet = event.packet) {
            is EntitySpawnS2CPacket -> (world.getEntityById(packet.entityId) as? GlobalEntityAddition)?.apply {
                updateTeleportedPosition(Vec3d(packet.x, packet.y, packet.z))
            }

            is EntityS2CPacket -> {
                val entity = packet.getEntity(world)
                val mixinEntity = entity as? GlobalEntityAddition

                mixinEntity?.apply {
                    if (!this.`liquidBounce$getPassedFirstUpdate`()) {
                        updateTeleportedPosition(entity.trackedPosition.pos)
                    }

                    val pos = this.`liquidBounce$getActualPosition`()

                    val trackedPos = TrackedPosition().apply { this.pos = pos }

                    val pos1 =
                        trackedPos.withDelta(packet.deltaX.toLong(), packet.deltaY.toLong(), packet.deltaZ.toLong())

                    updateTeleportedPosition(pos1)
                }
            }

            is EntityPositionS2CPacket -> (world.getEntityById(packet.entityId) as? GlobalEntityAddition)?.apply {
                updateTeleportedPosition(packet.change.position)
            }

            is EntityPositionSyncS2CPacket -> (world.getEntityById(packet.id) as? GlobalEntityAddition)?.apply {
                updateTeleportedPosition(packet.values.position)
            }
        }
    }

    /**
     * When we process packets, we want the delayed ones to be processed first before
     * the game proceeds with its own packet processing.
     *
     * @see net.minecraft.client.MinecraftClient.render
     *
     * profiler.push("scheduledExecutables");
     * this.runTasks();
     * profiler.pop();
     * profiler.push("tick");
     *
     */
    @Suppress("unused")
    private val handleTickPacketProcess = handler<TickPacketProcessEvent> {
        if (shouldCancelPackets()) {
            processPackets(shouldImmediatelyClear())
        } else {
            clear()
        }

        packetProcessQueue.removeIf {
            handlePacket(it)

            return@removeIf true
        }

        if (arePacketQueuesEmpty) {
            currentDelay = delay.random()
        }
    }

    val tickHandler = handler<GameTickEvent>(2) {
        mc.world?.entities?.forEach { entity ->
            if (entity !is LivingEntity) {
                return@forEach
            }

            (entity as? GlobalEntityAddition)?.run {
                if (!this.`liquidBounce$getPassedFirstUpdate`()) {
                    updateTeleportedPosition(entity.trackedPosition.pos)
                }
            }
        }
    }

    private fun GlobalEntityAddition.updateTeleportedPosition(target: Vec3d) {
        this.`liquidBounce$setActualPosition`(target)
        this.`liquidBounce$updateFirstUpdate`()
    }
}
