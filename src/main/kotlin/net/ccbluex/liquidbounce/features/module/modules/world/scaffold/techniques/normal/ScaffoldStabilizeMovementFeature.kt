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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.PlayerInput
import net.ccbluex.liquidbounce.utils.movement.copy
import net.ccbluex.liquidbounce.utils.movement.getDegreesRelativeToView
import net.ccbluex.liquidbounce.utils.movement.getDirectionalInputForDegrees
import net.minecraft.util.math.Vec3d

object ScaffoldStabilizeMovementFeature : ToggleableConfigurable(ScaffoldNormalTechnique, "StabilizeMovement",
    true) {
    private const val MAX_CENTER_DEVIATION: Double = 0.2
    private const val MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS: Double = 0.075

    @Suppress("unused")
    val moveEvent = handler<MovementInputEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        // Prevents the stabilization from giving the player a boost before jumping that cannot be corrected mid-air.
        if (event.input.jump && player.isOnGround) {
            return@handler
        }

        val optimalLine = ModuleScaffold.currentOptimalLine ?: return@handler
        val currentInput = event.input

        val nearestPointOnLine = optimalLine.getNearestPointTo(player.pos)

        val vecToLine = nearestPointOnLine.subtract(player.pos)
        val horizontalVelocity = Vec3d(player.velocity.x, 0.0, player.velocity.z)
        val isRunningTowardsLine = vecToLine.dotProduct(horizontalVelocity) > 0.0

        val maxDeviation =
            if (isRunningTowardsLine) {
                MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS
            } else {
                MAX_CENTER_DEVIATION
            }

        if (nearestPointOnLine.squaredDistanceTo(player.pos) < maxDeviation * maxDeviation) {
            return@handler
        }

        val dgs = getDegreesRelativeToView(nearestPointOnLine.subtract(player.pos), player.yaw)

        val newDirectionalInput = getDirectionalInputForDegrees(PlayerInput(), dgs, deadAngle = 0.0F)

        val frontalAxisBlocked = currentInput.forward || currentInput.backward
        val sagitalAxisBlocked = currentInput.right || currentInput.left

        event.input =
            event.input.copy(
                forward = if (frontalAxisBlocked) currentInput.forward else newDirectionalInput.forward,
                backward = if (frontalAxisBlocked) currentInput.backward else newDirectionalInput.backward,
                left = if (sagitalAxisBlocked) currentInput.left else newDirectionalInput.left,
                right = if (sagitalAxisBlocked) currentInput.right else newDirectionalInput.right,
            )
    }
}
