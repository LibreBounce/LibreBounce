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

package net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.ml.TensorflowIntegration
import net.ccbluex.liquidbounce.ml.TensorflowIntegration.models
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

/**
 * Record combat using [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.AimDebugRecorder] and
 * train model using recorded data with
 * [net.ccbluex.liquidbounce.features.command.commands.tensorflow.CommandTensorflow] which will
 * export a model to /models directory
 * and after restarting the client, this mode will be available to use.
 */
class TensorflowSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Tensorflow") {

    private val choices = choices("Model", 0) { local ->
        models.onChanged { _ ->
            local.choices = models.choices
        }

        models.choices.toTypedArray()
    }

    override fun limitAngleChange(
        rotationFactor: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val currentDirectionVector = currentRotation.directionVector
        val targetDirectionVector = targetRotation.directionVector
        val distance = entity?.let { entity -> player.boxedDistanceTo(entity) } ?: 0.0

        val model = choices.activeChoice as? TensorflowIntegration.TensorflowModel
            ?: run {
                chat(markAsError("No tensorflow model selected"))
                return currentRotation
            }

        ModuleDebug.debugParameter(this, "CurrentDirectionVector", currentDirectionVector)
        ModuleDebug.debugParameter(this, "TargetDirectionVector", targetDirectionVector)
        ModuleDebug.debugParameter(this, "Distance", distance)

        val input = floatArrayOf(
            currentDirectionVector.x.toFloat(), currentDirectionVector.y.toFloat(), currentDirectionVector.z.toFloat(),
            targetDirectionVector.x.toFloat(), targetDirectionVector.y.toFloat(), targetDirectionVector.z.toFloat(),
            distance.toFloat()
        )

        val deltaYaw = model.yawModel.predictSoftly(input)[0]
        val deltaPitch = model.pitchModel.predictSoftly(input)[0]

        ModuleDebug.debugParameter(this, "DeltaYaw", deltaYaw)
        ModuleDebug.debugParameter(this, "DeltaPitch", deltaPitch)

        return Rotation(
            currentRotation.yaw + deltaYaw,
            currentRotation.pitch + deltaPitch
        )
    }

    override fun howLongToReach(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        return 0
    }

}
