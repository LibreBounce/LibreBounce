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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.roundToInt

data class Rotation(
    var yaw: Float,
    var pitch: Float,
    var isNormalized: Boolean = false
) {

    companion object {
        val ZERO = Rotation(0f, 0f)
    }

    val rotationVec: Vec3d
        get() {
            val yawCos = MathHelper.cos(-yaw * 0.017453292f)
            val yawSin = MathHelper.sin(-yaw * 0.017453292f)
            val pitchCos = MathHelper.cos(pitch * 0.017453292f)
            val pitchSin = MathHelper.sin(pitch * 0.017453292f)
            return Vec3d((yawSin * pitchCos).toDouble(), (-pitchSin).toDouble(), (yawCos * pitchCos).toDouble())
        }

    /**
     * Fixes GCD and Modulo 360° at yaw
     *
     * @return [Rotation] with fixed yaw and pitch
     */
    fun normalize(): Rotation {
        if (isNormalized) return this

        val gcd = RotationManager.gcd

        // We use the [currentRotation] to calculate the normalized rotation, if it's null, we use
        // the player's rotation
        val currentRotation = RotationManager.currentRotation ?: player.rotation

        // get rotation differences
        val yawDifference = RotationManager.angleDifference(yaw, currentRotation.yaw)
        val pitchDifference = RotationManager.angleDifference(pitch, currentRotation.pitch)

        // proper rounding
        val g1 = (yawDifference / gcd).roundToInt() * gcd
        val g2 = (pitchDifference / gcd).roundToInt() * gcd

        // fix rotation
        val yaw = currentRotation.yaw + g1.toFloat()
        val pitch = currentRotation.pitch + g2.toFloat()

        return Rotation(yaw, pitch.coerceIn(-90f, 90f), isNormalized = true)
    }

}

data class VecRotation(val rotation: Rotation, val vec: Vec3d)
