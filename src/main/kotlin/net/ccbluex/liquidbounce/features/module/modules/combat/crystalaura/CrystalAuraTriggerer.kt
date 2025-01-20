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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.injection.mixins.minecraft.network.MixinClientPlayNetworkHandler
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max

/**
 * Catches events that should start a new place or break action.
 *
 * This is basically the managing class of the crystal aura.
 *
 * Mixins: [MixinClientPlayNetworkHandler]
 */
object CrystalAuraTriggerer : Configurable("Triggers"), EventListener, MinecraftShortcuts {

    // avoids grim multi action flags
    val notWhileUsingItem by boolean("NotWhileUsingItem", false)

    /**
     * Options Define when the CA should run. Only tick is the most legit.
     */

    val tick by boolean("Tick", true)
    val blockChange by boolean("BlockChange", true)
    val clientBlockBreak by boolean("ClientBlockBreak", true)
    val crystalSpawn by boolean("CrystalSpawn", true)
    val crystalDestroy by boolean("CrystalDestroy", true)
    val entityMove by boolean("EntityMove", true)
    val selfMove by boolean("SelfMove", false)

    val offThread by boolean("Off-Thread", true) // TODO toggle configurable off-thread

    private val service = Executors.newSingleThreadExecutor()

    @Volatile
    private var currentPlaceTask: Future<*>? = null
    @Volatile
    private var currentBreakTask: Future<*>? = null

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        if (!tick) {
            return@handler
        }

        if (offThread) {
            runDestroy { SubmoduleCrystalDestroyer.tick() }
            runPlace { SubmoduleCrystalPlacer.tick() }
        } else {
            // Make the crystal destroyer run
            SubmoduleCrystalDestroyer.tick()
            // Make the crystal placer run
            SubmoduleCrystalPlacer.tick()
            if (!SubmoduleIdPredict.enabled) {
                // Make the crystal destroyer run
                SubmoduleCrystalDestroyer.tick()
            }
        }
    }

    @Suppress("unused")
    private val packetListener = handler<PacketEvent>(-1) { event ->
        val packet = event.packet
        /* if (packet is PlaySoundFromEntityS2CPacket && packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE && crystalDestroy) {
             handling.runPlace { SubmoduleCrystalPlacer.tick(intArrayOf(packet.entityId)) }
         }*/ /*else if (packet is EntitySpawnS2CPacket && packet.entityType == EntityType.END_CRYSTAL && crystalSpawn) {
            handling.runDestroy {
                var entity = world.getEntityById(packet.entityId)
                entity?.let { entity1 ->
                    if (entity1 !is EndCrystalEntity) {
                        return@runDestroy
                    }
                } ?: run {
                    entity = packet.entityType.create(world, SpawnReason.LOAD)
                    entity!!.onSpawnPacket(packet)
                }

                SubmoduleCrystalDestroyer.tick(entity as EndCrystalEntity) }
            if (SubmoduleSetDead.enabled) {
                handling.runPlace { SubmoduleCrystalPlacer.tick() }
            }
        } else */if (packet is PlayerMoveC2SPacket && selfMove) {
            mc.execute {
                runDestroy { SubmoduleCrystalDestroyer.tick() }
                runPlace { SubmoduleCrystalPlacer.tick() }
            }
        } else if (packet is EntitiesDestroyS2CPacket && crystalDestroy) {
            if (packet.entityIds.any { world.getEntityById(it) is EndCrystalEntity }) { // TODO this needs range checks!
                runPlace { SubmoduleCrystalPlacer.tick(packet.entityIds.toIntArray()) }
            }
        }
    }

    fun postDestroyHandler(packet: PlaySoundFromEntityS2CPacket) {
        if (packet.sound == SoundEvents.ENTITY_GENERIC_EXPLODE) {
            postDestroyHandler()
        }
    }

/*    fun postDestroyHandler(packet: EntitiesDestroyS2CPacket) {
        if (packet.entityIds == SoundEvents.ENTITY_GENERIC_EXPLODE) {
            postDestroyHandler()
        }
    }*/

    private fun postDestroyHandler() {
        if (!running || !crystalDestroy) {
            return
        }

        runPlace { SubmoduleCrystalPlacer.tick() }
    }

    fun postSpawnHandler(packet: EntitySpawnS2CPacket) {
        if (!running || packet.entityType != EntityType.END_CRYSTAL || !crystalSpawn) {
            return
        }

        runDestroy {
            val entity = world.getEntityById(packet.entityId)
            if (entity !is EndCrystalEntity) {
                return@runDestroy
            }

            SubmoduleCrystalDestroyer.tick(/*entity*/)
        }

        if (SubmoduleSetDead.enabled) {
            runPlace { SubmoduleCrystalPlacer.tick() }
        }
    }

    fun postMoveHandler(packet: EntityPositionS2CPacket) {
        if (!running || !entityMove) {
            return
        }

        val entity = world.getEntityById(packet.entityId) ?: return
        if (player.eyePos.squaredDistanceTo(entity.pos) > ModuleCrystalAura.targetTracker.range.sq()) {
            return
        }

        runDestroy { SubmoduleCrystalDestroyer.tick() }
        runPlace { SubmoduleCrystalPlacer.tick() }
    }

    fun postBlockUpdateHandler(packet: BlockUpdateS2CPacket) {
        if (!running || !blockChange || !packet.state.isAir) {
            return
        }

        tickIfInRange(
            packet.pos,
            player.eyePos,
            max(SubmoduleCrystalPlacer.getMaxRange(), SubmoduleCrystalDestroyer.getMaxRange()).sq() + 1.0
        )
    }

    fun postChunkUpdateHandler(packet: ChunkDeltaUpdateS2CPacket) {
        if (!running || !blockChange) {
            return
        }

        val eyePos = player.eyePos
        val rangeSq = max(SubmoduleCrystalPlacer.getMaxRange(), SubmoduleCrystalDestroyer.getMaxRange()).sq() + 1.0
        packet.visitUpdates { blockPos, blockState ->
            if (blockState.isAir && tickIfInRange(blockPos, eyePos, rangeSq)) {
                return@visitUpdates
            }
        }
    }

    private fun tickIfInRange(blockPos: BlockPos, eyePos: Vec3d, rangeSq: Double): Boolean {
        val distance = eyePos.squaredDistanceTo(
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble()
        )

        if (distance < rangeSq) {
            runPlace { SubmoduleCrystalPlacer.tick() }
            return true
        }

        return false
    }

    fun clientBreakHandler() {
        if (!running || !clientBlockBreak) {
            return
        }

        runPlace { SubmoduleCrystalPlacer.tick() }
    }

    private fun runPlace(runnable: Runnable) {
        currentPlaceTask?.let {
            if (!it.isDone) {
                return
            }
        }

        if (offThread) {
            currentPlaceTask = service.submit(runnable)
        } else {
            currentPlaceTask = null
            mc.execute(runnable)
        }
    }

    private fun runDestroy(runnable: Runnable) {
        currentBreakTask?.let {
            if (!it.isDone) {
                return
            }
        }

        if (offThread) {
            currentBreakTask = service.submit(runnable)
        } else {
            currentBreakTask = null
            mc.execute(runnable)
        }
    }

    fun canCache() = !offThread && tick && !blockChange && !clientBlockBreak && !crystalSpawn && !crystalDestroy && !entityMove && !selfMove

    override val running: Boolean
        get() = ModuleCrystalAura.running
            && !CombatManager.shouldPauseCombat
            && (!player.isUsingItem || !notWhileUsingItem)

}
