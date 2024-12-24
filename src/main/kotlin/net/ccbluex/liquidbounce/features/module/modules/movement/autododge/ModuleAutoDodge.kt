/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.autododge

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.EventScheduler
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object ModuleAutoDodge : ClientModule("AutoDodge", Category.COMBAT) {
    private object AllowRotationChange : ToggleableConfigurable(this, "AllowRotationChange", false) {
        val allowJump by boolean("AllowJump", true)
    }

    private object AllowTimer : ToggleableConfigurable(this, "AllowTimer", false) {
        val timerSpeed by float("TimerSpeed", 2.0F, 1.0F..10.0F, suffix = "x")
    }

    init {
        tree(AllowRotationChange)
        tree(AllowTimer)
    }

    @Suppress("unused")
    val tickRep = handler<MovementInputEvent> { event ->
        // We aren't where we are because of blink. So this module shall not cause any disturbance in that case.
        if (ModuleBlink.running) {
            return@handler
        }
        if (ModuleMurderMystery.disallowsArrowDodge()) {
            return@handler
        }

        val world = world

        val arrows = findFlyingArrows(world)

        val simulatedPlayer = CachedPlayerSimulation(PlayerSimulationCache.getSimulationForLocalPlayer())

        val inflictedHit =
            getInflictedHits(simulatedPlayer, arrows, hitboxExpansion = DodgePlanner.SAFE_DISTANCE_WITH_PADDING)
                ?: return@handler

        val dodgePlan =
            planEvasion(DodgePlannerConfig(allowRotations = AllowRotationChange.enabled), inflictedHit)
                ?: return@handler

        event.directionalInput = dodgePlan.directionalInput

        dodgePlan.yawChange?.let { yawChange ->
            player.yaw = yawChange
        }

        if (dodgePlan.shouldJump && AllowRotationChange.allowJump && player.isOnGround) {
            EventScheduler.schedule<MovementInputEvent>(ModuleScaffold) {
                it.jump = true
            }
        }

        if (AllowTimer.enabled && dodgePlan.useTimer) {
            Timer.requestTimerSpeed(AllowTimer.timerSpeed, Priority.IMPORTANT_FOR_PLAYER_LIFE, this@ModuleAutoDodge)
        }
    }

    fun findFlyingArrows(world: ClientWorld): List<ArrowEntity> {
        return world.entities.mapNotNull {
            if (it !is ArrowEntity) {
                return@mapNotNull null
            }

            if (it.isInGround()) {
                return@mapNotNull null
            }

            return@mapNotNull it
        }
    }

    fun <T : PlayerSimulation> getInflictedHits(
        simulatedPlayer: T,
        arrows: List<ArrowEntity>,
        maxTicks: Int = 80,
        hitboxExpansion: Double = 0.7,
    ): HitInfo? {
        val simulatedArrows = arrows.map { SimulatedArrow(world, it.pos, it.velocity, false) }

        for (i in 0 until maxTicks) {
            simulatedPlayer.tick()

            simulatedArrows.forEachIndexed { arrowIndex, arrow ->
                if (arrow.inGround) {
                    return@forEachIndexed
                }

                val lastPos = arrow.pos

                arrow.tick()

                val playerHitBox =
                    Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
                        .expand(hitboxExpansion)
                        .offset(simulatedPlayer.pos)
                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let { hitPos ->
                    return HitInfo(i, arrows[arrowIndex], hitPos, lastPos, arrow.velocity)
                }
            }
        }

        return null
    }

    data class EvadingPacket(
        val idx: Int,
        /**
         * Ticks until impact. Null if evaded
         */
        val ticksToImpact: Int?
    )

    /**
     * Returns the index of the first position packet that avoids all arrows in the next X seconds
     */
    fun findAvoidingArrowPosition(): EvadingPacket? {
        var packetIndex = 0

        var lastPosition: Vec3d? = null

        var bestPacketPosition: Vec3d? = null
        var bestPacketIdx: Int? = null
        var bestTimeToImpact = 0

        for (position in PacketQueueManager.positions) {
            packetIndex += 1

            // Process packets only if they are at least some distance away from each other
            if (lastPosition != null) {
                if (lastPosition.squaredDistanceTo(position) < 0.9 * 0.9) {
                    continue
                }
            }

            lastPosition = position

            val inflictedHit = getInflictedHit(position)

            if (inflictedHit == null) {
                return EvadingPacket(packetIndex - 1, null)
            } else if (inflictedHit.tickDelta > bestTimeToImpact) {
                bestTimeToImpact = inflictedHit.tickDelta
                bestPacketIdx = packetIndex - 1
                bestPacketPosition = position
            }
        }

        // If the evading packet is less than one player hitbox away from the current position, we should rather
        // call the evasion a failure
        if (bestPacketIdx != null && bestPacketPosition!!.squaredDistanceTo(lastPosition!!) > 0.9) {
            return EvadingPacket(bestPacketIdx, bestTimeToImpact)
        }

        return null
    }

    fun getInflictedHit(pos: Vec3d): HitInfo? {
        val arrows = findFlyingArrows(net.ccbluex.liquidbounce.utils.client.world)
        val playerSimulation = RigidPlayerSimulation(pos)

        return getInflictedHits(playerSimulation, arrows, maxTicks = 40)
    }

    data class HitInfo(
        val tickDelta: Int,
        val arrowEntity: ArrowEntity,
        val hitPos: Vec3d,
        val prevArrowPos: Vec3d,
        val arrowVelocity: Vec3d,
    )
}
