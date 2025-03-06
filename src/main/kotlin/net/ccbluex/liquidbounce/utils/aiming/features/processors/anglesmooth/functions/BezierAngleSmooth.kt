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
 *
 */
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.functions

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.random

class BezierAngleSmooth(parent: ChoiceConfigurable<*>) : FunctionAngleSmooth("Bezier", parent) {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f, 0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f, 0.0f..180f)
    private val controlPoint by float("ControlPoint", 0.5f, 0.0f..1.0f)

    /**
     * Calculate the factors for the rotation towards the target rotation.
     *
     * TODO: Change this to be a 0.0 to 1.0 range instead of 0.0 to 180.0
     *
     * @param currentRotation The current rotation
     * @param targetRotation The target rotation
     */
    override fun calculateFactors(
        rotationTarget: RotationTarget?,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Pair<Float, Float> {
        val rotationDifference = currentRotation.angleTo(targetRotation)

        val horizontalFactor = calculateFactor(rotationDifference, horizontalTurnSpeed.random().toDouble())
        val verticalFactor = calculateFactor(rotationDifference, verticalTurnSpeed.random().toDouble())

        return horizontalFactor to verticalFactor
    }

    private fun calculateFactor(rotationDifference: Float, turnSpeed: Double): Float {
        val t = (rotationDifference / 180f).coerceIn(0f, 1f)
        val bezierSpeed = bezierInterpolate(0f, controlPoint, 1f, 1 - t) * turnSpeed

        return bezierSpeed.toFloat().coerceIn(0f, 180f)
    }

    private fun bezierInterpolate(start: Float, control: Float, end: Float, t: Float): Float {
        return (1 - t) * (1 - t) * start + 2 * (1 - t) * t * control + t * t * end
    }

}
