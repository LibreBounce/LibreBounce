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

import net.ccbluex.liquidbounce.utils.aiming.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class Rotation(
    var yaw: Float,
    var pitch: Float,
    var isNormalized: Boolean = false
) {

    companion object {
        val ZERO = Rotation(0f, 0f)

        fun lookingAt(point: Vec3d, from: Vec3d): Rotation {
            return fromRotationVec(point.subtract(from))
        }

        fun fromRotationVec(lookVec: Vec3d): Rotation {
            val diffX = lookVec.x
            val diffY = lookVec.y
            val diffZ = lookVec.z

            return Rotation(
                MathHelper.wrapDegrees(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                MathHelper.wrapDegrees((-Math.toDegrees(atan2(diffY, sqrt(diffX * diffX + diffZ * diffZ)))).toFloat())
            )
        }
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

        val gcd = RotationUtil.gcd

        // We use the [currentRotation] to calculate the normalized rotation, if it's null, we use
        // the player's rotation
        val currentRotation = RotationManager.currentRotation ?: player.rotation

        // get rotation differences
        val diff = currentRotation.rotationDeltaTo(this)

        // proper rounding
        val g1 = (diff.deltaYaw / gcd).roundToInt() * gcd
        val g2 = (diff.deltaPitch / gcd).roundToInt() * gcd

        // fix rotation
        val yaw = currentRotation.yaw + g1.toFloat()
        val pitch = currentRotation.pitch + g2.toFloat()

        return Rotation(yaw, pitch.coerceIn(-90f, 90f), isNormalized = true)
    }

    /**
     * Calculates the angle between this and the other rotation.
     *
     * @return angle in degrees
     */
    fun angleTo(other: Rotation): Float {
        return rotationDeltaTo(other).length().coerceAtMost(180.0F)
    }

    /**
     * Calculates what angles would need to be added to arrive at [other].
     *
     * Wrapped 360°
     */
    fun rotationDeltaTo(other: Rotation): RotationDelta {
        return RotationDelta(
            angleDifference(other.yaw, this.yaw),
            angleDifference(other.pitch, this.pitch)
        )
    }

}

data class RotationDelta(val deltaYaw: Float, val deltaPitch: Float) {
    fun length() = hypot(deltaYaw, deltaPitch)
}
data class VecRotation(val rotation: Rotation, val vec: Vec3d)
