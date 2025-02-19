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
package net.ccbluex.liquidbounce.deeplearn.generators

import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.util.math.Vec2f
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.math.exp

object BaseDataGenerator : DataGenerator {

    private const val H_SPEED = 95.0
    private const val V_SPEED = 35.0

    // Sigmoid function parameters
    private const val STEEPNESS = 10f
    private const val MIDPOINT = 0.3f

    override fun generateData(samples: Int): List<TrainingData> {
        val trainingData = mutableListOf<TrainingData>()

        repeat(samples) {
            sample(trainingData)
        }

        return trainingData
    }

    private fun sample(trainingData: MutableList<TrainingData>) {
        val target = starting()
        val targetVector = target.directionVector
        val random = SecureRandom()

        var current = starting()
        var previous = starting()

        val ti = random.nextGaussian(0.4012, 2.2643).toFloat()
        val to = random.nextGaussian(15.5179, 11.3291).toFloat()
        val e = random.nextGaussian(2.7127, 1.2936).toFloat()
        val v = random.nextGaussian(2.7180, 1.2915).toFloat()

        while (current.angleTo(target) > 1.0E-5f) {
            val next = this.next(current, target)
            val delta = current.rotationDeltaTo(next)

            trainingData += TrainingData(
                current.directionVector,
                previous.directionVector,
                targetVector,
                Vec2f(delta.deltaYaw, delta.deltaPitch),
//                ti.toInt(),
//                to.toInt(),
                e,
                v
            )

            previous = current
            current = next
        }
    }

    private fun starting() = Rotation(
        yaw = (-180f..180f).random().toFloat(),
        pitch = (-90f..90f).random().toFloat()
    )

    private fun next(
        clientRotation: Rotation,
        targetRotation: Rotation,
    ): Rotation {
        val diff = clientRotation.rotationDeltaTo(targetRotation)
        val rotationDifference = diff.length()
        val factorH = computeFactor(rotationDifference, H_SPEED)
        val straightLineYaw = (abs(diff.deltaYaw / rotationDifference) * factorH).toFloat()
        val factorV = computeFactor(rotationDifference, V_SPEED)
        val straightLinePitch = (abs(diff.deltaPitch / rotationDifference) * factorV).toFloat()

        return Rotation(
            clientRotation.yaw + diff.deltaYaw.coerceIn(-straightLineYaw, straightLineYaw),
            clientRotation.pitch + diff.deltaPitch.coerceIn(-straightLinePitch, straightLinePitch)
        )
    }

    private fun computeFactor(rotationDifference: Float, speed: Double): Float {
        val scaledDifference = rotationDifference / 120f
        val sigmoid = 1 / (1 + exp((-STEEPNESS * (scaledDifference - MIDPOINT)).toDouble()))

        val interpolatedSpeed = sigmoid * speed
        return interpolatedSpeed.toFloat()
            .coerceAtLeast(0f)
            .coerceAtMost(180f)
    }

}
