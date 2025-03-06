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
 *
 */

package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.functions

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import kotlin.math.exp

class SigmoidAngleSmooth(parent: ChoiceConfigurable<*>) : FunctionAngleSmooth("Sigmoid", parent) {

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f,
        0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f,
        0.0f..180f)

    private val steepness by float("Steepness", 10f, 0.0f..20f)
    private val midpoint by float("Midpoint", 0.3f, 0.0f..1.0f)

    override fun calculateFactors(
        rotationTarget: RotationTarget?,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Pair<Float, Float> {
        val rotationDifference = currentRotation.angleTo(targetRotation)

        val horizontalFactor = computeFactor(rotationDifference, horizontalTurnSpeed.random().toDouble())
        val verticalFactor = computeFactor(rotationDifference, verticalTurnSpeed.random().toDouble())

        return horizontalFactor to verticalFactor
    }

    private fun computeFactor(rotationDifference: Float, turnSpeed: Double): Float {
        val t = rotationDifference / 120f
        val expr = 1 / (1 + exp((-steepness * (t - midpoint)).toDouble()))
        val speed = expr * turnSpeed

        return speed.toFloat().coerceIn(0f, 180f)
    }

}
