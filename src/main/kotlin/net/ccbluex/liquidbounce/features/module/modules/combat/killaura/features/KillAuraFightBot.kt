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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.clickScheduler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import kotlin.math.min

/**
 * A fight bot, fights for you, probably better than you. Lol.
 */
object KillAuraFightBot : ToggleableConfigurable(ModuleKillAura, "FightBot", false) {

    private val opponentRange by float("OpponentRange", 3f, 0.1f..5f)
    private val dangerousYawDiff by float("DangerousYaw", 55f, 0f..90f, suffix = "°")

    private val runawayOnCooldown by boolean("RunawayOnCooldown", true)

    private val autoSprint by boolean("AutoSprint", true)
    private val autoJump by boolean("AutoJump", true)

    /**
     * Enables Minecraft Auto Jump logic,
     * no matter if the game option is enabled or not.
     */
    val minecraftAutoJump
        get() = running && autoJump

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        val target = targetTracker.lockedOnTarget ?: return@handler

        val playerPosition = player.pos
        val targetPosition = target.pos
        val distance = playerPosition.distanceTo(targetPosition)
        val range = min(ModuleKillAura.range, distance.toFloat())
        val outOfDistance = distance  > opponentRange

        val targetRotation = target.rotation.copy(pitch = 0.0f)
        val requiredTargetRotation = Rotation.lookingAt(playerPosition, target.eyePos).copy(pitch = 0.0f)

        // Allow combo from the side
        val outOfDanger = targetRotation.angleTo(requiredTargetRotation) > dangerousYawDiff

        // (Target position + Target rotation vector * distance)
        val targetLookPosition = targetPosition.add(
            targetRotation.rotationVec * range.toDouble()
        )

        val goal = if (runawayOnCooldown && !clickScheduler.isClickOnNextTick()) {
            // Subtract the target rotation vector from the player position, which means
            // we run away from the target
            val playerRunawayPosition = playerPosition.add(requiredTargetRotation.rotationVec * range.toDouble())

            playerRunawayPosition
        } else {
            (0..360 step 10).mapNotNull { yaw ->
                val rotation = Rotation(yaw = yaw.toFloat(), pitch = 0.0F)
                val position = targetPosition.add(rotation.rotationVec * range.toDouble())

                val isInAngle = rotation.angleTo(targetRotation) <= dangerousYawDiff

                ModuleDebug.debugGeometry(
                    this, "Possible Position $yaw",
                    ModuleDebug.DebuggedPoint(position, if (!isInAngle) Color4b.GREEN else Color4b.RED)
                )

                // Filter out yaw that is too close to the target yaw
                if (isInAngle) {
                    return@mapNotNull null
                }

                position
            }
                // Sort by distance to target look position
                .sortedBy { position -> position.squaredDistanceTo(targetLookPosition) }
                // Then find the closest to the player
                .minByOrNull { position -> position.squaredDistanceTo(playerPosition) } ?: targetLookPosition
        }

        ModuleDebug.debugGeometry(
            this,
            "Target Look Position",
            ModuleDebug.DebuggedPoint(targetLookPosition, Color4b.BLACK, size = 0.4)
        )
        ModuleDebug.debugGeometry(this, "Goal", ModuleDebug.DebuggedPoint(goal, Color4b.BLUE, size = 0.4))

        val dgs = getDegreesRelativeToView(goal.subtract(player.pos), player.yaw)
        val directionInput = getDirectionalInputForDegrees(event.directionalInput, dgs, deadAngle = 20.0F)
        event.directionalInput = directionInput

        if (autoJump) {
            if (player.horizontalCollision || outOfDistance && !outOfDanger) {
                event.jump = true
            }
        }
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(
        priority = FIRST_PRIORITY
    ) { event ->
        if (!autoSprint) {
            return@handler
        }

        if (!event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

    /**
     * Get the rotation to look at the target while moving towards it
     */
    fun getMovementRotation(): Rotation? {
        val target = targetTracker.lockedOnTarget ?: return null

        val movementYaw = getMovementDirectionOfInput(player.yaw, DirectionalInput(player.input))
        val lookAtRotation = Rotation.lookingAt(point = target.box.center, from = player.eyePos)

        return Rotation(movementYaw, lookAtRotation.pitch)
    }

}
