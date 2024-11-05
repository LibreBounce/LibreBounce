/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.anglesmooth

import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

class AccelerationSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Acceleration") {

    private val yawAcceleration by floatRange("YawAcceleration", 20f..25f, 1f..100f)
    private val pitchAcceleration by floatRange("PitchAcceleration", 20f..25f, 1f..100f)
    private val yawAccelerationError by float("YawAccelerationError", 0.1f, 0f..1f)
    private val pitchAccelerationError by float("PitchAccelerationError", 0.1f, 0f..1f)
    private val yawConstantError by float("YawConstantError", 0.1f, 0f..10f)
    private val pitchConstantError by float("PitchConstantError", 0.1f, 0f..10f)

    override fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val prevYawDiff = RotationManager.angleDifference(currentRotation.yaw, prevRotation.yaw)
        val prevPitchDiff = RotationManager.angleDifference(currentRotation.pitch, prevRotation.pitch)
        val yawDiff = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDiff = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)

        val (newYawDiff, newPitchDiff) = computeTurnSpeed(
            prevYawDiff,
            prevPitchDiff,
            yawDiff,
            pitchDiff,
        )

        return Rotation(
            currentRotation.yaw + newYawDiff,
            currentRotation.pitch + newPitchDiff
        )
    }

    override fun howLongToReach(currentRotation: Rotation, targetRotation: Rotation): Int {
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val prevYawDiff = RotationManager.angleDifference(currentRotation.yaw, prevRotation.yaw)
        val prevPitchDiff = RotationManager.angleDifference(currentRotation.pitch, prevRotation.pitch)
        val yawDiff = RotationManager.angleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDiff = RotationManager.angleDifference(targetRotation.pitch, currentRotation.pitch)

        val (computedH, computedV) = computeTurnSpeed(
            prevYawDiff,
            prevPitchDiff,
            yawDiff,
            pitchDiff,
        )
        val lowest = min(computedH, computedV)

        if (lowest <= 0.0) {
            return 0
        }

        if (yawDiff == 0f && pitchDiff == 0f) {
            return 0
        }

        return (hypot(abs(yawDiff), abs(pitchDiff)) / lowest).roundToInt()
    }

    private fun computeTurnSpeed(
        prevYawDiff: Float,
        prevPitchDiff: Float,
        yawDiff: Float,
        pitchDiff: Float,
    ): Pair<Float, Float> {
        val yawAccel = RotationManager.angleDifference(yawDiff, prevYawDiff)
            .coerceIn(-yawAcceleration.random().toFloat(), yawAcceleration.random().toFloat())
        val pitchAccel = RotationManager.angleDifference(pitchDiff, prevPitchDiff)
            .coerceIn(-pitchAcceleration.random().toFloat(), pitchAcceleration.random().toFloat())

        val yawError = yawAccel * yawErrorMulti() + yawConstantError()
        val pitchError = pitchAccel * pitchErrorMulti() + pitchConstantError()

        return (prevYawDiff + yawAccel + yawError) to (prevPitchDiff + pitchAccel + pitchError)
    }

    private fun yawErrorMulti() = (-yawAccelerationError..yawAccelerationError).random().toFloat()
    private fun pitchErrorMulti() = (-pitchAccelerationError..pitchAccelerationError).random().toFloat()
    private fun yawConstantError() = (-yawConstantError..yawConstantError).random().toFloat()
    private fun pitchConstantError() = (-pitchConstantError..pitchConstantError).random().toFloat()
}
