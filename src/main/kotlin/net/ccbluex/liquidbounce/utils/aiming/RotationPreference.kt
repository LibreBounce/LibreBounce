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
package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.math.Vec3d


interface RotationPreference : Comparator<Rotation> {
    fun getPreferredSpot(
        eyesPos: Vec3d,
        range: Double,
    ): Vec3d
}

class LeastDifferencePreference(
    private val baseRotation: Rotation,
    private val basePoint: Vec3d? = null,
) : RotationPreference {
    override fun getPreferredSpot(eyesPos: Vec3d, range: Double): Vec3d {
        if (this.basePoint != null) {
            return this.basePoint
        }

        return eyesPos + this.baseRotation.rotationVec * range
    }

    override fun compare(o1: Rotation, o2: Rotation): Int {
        val rotationDifferenceO1 = baseRotation.angleTo(o1)
        val rotationDifferenceO2 = baseRotation.angleTo(o2)

        return rotationDifferenceO1.compareTo(rotationDifferenceO2)
    }

    companion object {
        val LEAST_DISTANCE_TO_CURRENT_ROTATION: LeastDifferencePreference
            get() = LeastDifferencePreference(RotationManager.currentRotation ?: player.rotation)

        fun leastDifferenceToLastPoint(eyes: Vec3d, point: Vec3d): LeastDifferencePreference {
            return LeastDifferencePreference(Rotation.lookingAt(point, from = eyes), point)
        }
    }

}
