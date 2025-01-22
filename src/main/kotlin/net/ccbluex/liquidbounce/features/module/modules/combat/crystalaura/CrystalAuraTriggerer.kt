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
import net.ccbluex.liquidbounce.interfaces.EntitiesDestroyS2CPacketAddition
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.EntityType
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.max

// TODO comment
// TODO no duplicate place and break options per tick in both place and break
/**
 * Catches events that should start a new place or break action.
 *
 * This is basically the managing class of the crystal aura.
 *
 * Mixins: [MixinClientPlayNetworkHandler]
 */
object CrystalAuraTriggerer : Configurable("Triggers"), EventListener, MinecraftShortcuts {

    // avoids grim multi action flags
    private val notWhileUsingItem by boolean("NotWhileUsingItem", false)

    /**
     * Options Define when the CA should run. Only tick is the most legit.
     */

    /**
     * Runs placing and destroying every tick.
     */
    private val tick by boolean("Tick", true)

    /**
     * Runs placing right when a block was broken in the area where the aura operates.
     * This can help to block the surround of enemies with immediate placements.
     */
    private val blockChange by boolean("BlockChange", true)

    /**
     * Same as block change, but it will run even earlier but just for blocks that are broken client side.
     * If you use packet mine on normal mode, make sure to enable ClientSideSet in order to make this work properly.
     */
    private val clientBlockBreak by boolean("ClientBlockBreak", true)

    /**
     * Runs destroying when the information, that a crystal is spawned is received.
     *
     * When Set-Dead is enabled, this will also run placing.
     */
    private val crystalSpawn by boolean("CrystalSpawn", true)

    /**
     * Runs placing when the information, that a crystal is removed is received.
     */
    private val crystalDestroy by boolean("CrystalDestroy", true)

    /**
     * Runs placing when an explosion sound is received.
     */
    private val explodeSound by boolean("ExplodeSound", false)

    /**
     * Runs placing when an entity moves.
     */
    private val entityMove by boolean("EntityMove", true)

    /**
     * Runs placing when youself moves.
     */
    private val selfMove by boolean("SelfMove", false)

    /**
     * Runs the calculations on a separate thread avoiding overhead on the render thread.
     */
    private val offThread by boolean("Off-Thread", true)

    private val service = Executors.newSingleThreadExecutor()

    /**
     * The currently executed placement task.
     */
    private var currentPlaceTask: Future<*>? = null

    /**
     * The currently executed destroy task.
     */
    private var currentDestroyTask: Future<*>? = null

    @Suppress("unused")
    private val simulatedTickHandler = handler<RotationUpdateEvent> {
        //chat("Tick " + LocalTime.now().format(formatter))

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
        if (packet is PlayerMoveC2SPacket && selfMove) {
            mc.execute {
                runDestroy { SubmoduleCrystalDestroyer.tick() }
                runPlace { SubmoduleCrystalPlacer.tick() }
            }
        } else if (packet is EntitiesDestroyS2CPacket && crystalDestroy) {
            val maxRangeSq = SubmoduleCrystalPlacer.getMaxRange().sq()
            if (packet.entityIds.any {
                val entity = world.getEntityById(it)
                entity is EndCrystalEntity && entity.pos.squaredDistanceTo(player.pos) <= maxRangeSq
            }) {
                (packet as EntitiesDestroyS2CPacketAddition).`liquid_bounce$setContainsCrystal`()
            }
        }
    }

    fun postDestroyHandler(packet: EntitiesDestroyS2CPacket) {
        if (!running || !crystalDestroy || !(packet as EntitiesDestroyS2CPacketAddition).`liquid_bounce$containsCrystal`()) {
            return
        }

        runPlace { SubmoduleCrystalPlacer.tick() }
    }

    fun postSoundHandler(packet: PlaySoundFromEntityS2CPacket) {
        if (!running || !explodeSound || packet.sound != SoundEvents.ENTITY_GENERIC_EXPLODE) {
            return
        }

        world.getEntityById(packet.entityId)?.let {
            // don't place if the sound is too far away
            val maxRangeSq = SubmoduleCrystalPlacer.getMaxRange().sq()
            if (it.pos.squaredDistanceTo(player.pos) > maxRangeSq) {
                return
            }
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

    private var maxId = 0 // TODO TESTING ONLY, REMOVE
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS")

    private fun runPlace(runnable: Runnable) {
        val id = ++maxId

        currentPlaceTask?.let {
            if (!it.isDone) {
                print("$id canceled because current is not done!")
                return
            }
        }

        if (offThread) {
            currentPlaceTask = service.submit {
                //chat("Starting place ($id)..." + LocalTime.now().format(formatter))
                runnable.run()
                //chat("Finished place ($id)..." + LocalTime.now().format(formatter))
            }
        } else {
            currentPlaceTask?.cancel(true)
            currentPlaceTask = null
            mc.execute {
                //chat("Starting place ($id)..." + LocalTime.now().format(formatter))
                runnable.run()
                //chat("Finished place ($id)..." + LocalTime.now().format(formatter))
            }
        }
    }

    private fun runDestroy(runnable: Runnable) {
        val id = ++maxId

        currentDestroyTask?.let {
            if (!it.isDone) {
                return
            }
        }

        if (offThread) {
            currentDestroyTask = service.submit {
                //chat("Starting break ($id)..." + LocalTime.now().format(formatter))
                runnable.run()
                //chat("Finished break ($id)..." + LocalTime.now().format(formatter))
            }
        } else {
            currentDestroyTask?.cancel(true)
            currentDestroyTask = null
            mc.execute {
                //chat("Starting break ($id)..." + LocalTime.now().format(formatter))
                runnable.run()
                //chat("Finished break ($id)..." + LocalTime.now().format(formatter))
            }
        }
    }

    /**
     * We should not cache if the calculation is done off-tread and the cache gets cleared on tick, but the calculation
     * which runs on a separate thread could run parallel to the cleaning.
     *
     * Additionally, the caching is not needed if the calculation is multithreaded and therefore already has no
     * performance impact on the render thread.
     */
    fun canCache() = !offThread &&
        tick &&
        !blockChange &&
        !clientBlockBreak &&
        !crystalSpawn &&
        !crystalDestroy &&
        !entityMove &&
        !selfMove

    /**
     * Also pauses when the combat manager tells combat modules to pause or option
     * (e.g. [notWhileUsingItem]) require it.
     */
    override val running: Boolean
        get() = ModuleCrystalAura.running
            && !CombatManager.shouldPauseCombat
            && (!player.isUsingItem || !notWhileUsingItem)

}
