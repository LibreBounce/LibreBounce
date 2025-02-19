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
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelHolster.models
import net.ccbluex.liquidbounce.deeplearn.data.MAXIMUM_TRAINING_AGE
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.deeplearn.models.MinaraiModel
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.min

var previousEntityDistance = 0.0f

/**
 * Record combat using [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiRecorder] and
 * train model using recorded data with
 * [net.ccbluex.liquidbounce.features.command.commands.dl.CommandModels] which will
 * export a model to /models directory
 * and after restarting the client, this mode will be available to use.
 */
class MinaraiSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("Minarai") {

    private val choices = choices("Model", 0) { local ->
        models.onChanged { _ ->
            local.choices = models.choices
        }

        models.choices.toTypedArray()
    }

    private class OutputMultiplier : Configurable("OutputMultiplier") {
        var yawMultiplier by float("Yaw", 1.5f, 0.5f..2f)
        var pitchMultiplier by float("Pitch", 1f, 0.5f..2f)
    }

    private val outputMultiplier = tree(OutputMultiplier())

    override fun limitAngleChange(
        rotationFactor: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        if (!DeepLearningEngine.isInitialized) {
            chat(markAsError("No deep learning engine found."))
            return currentRotation
        }

        val entityDistance = entity?.let { entity -> player.squaredDistanceTo(entity).toFloat() }
            ?: previousEntityDistance
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val totalDelta = currentRotation.rotationDeltaTo(targetRotation)
        val velocityDelta = prevRotation.rotationDeltaTo(currentRotation)

        val model = choices.activeChoice as? MinaraiModel
            ?: run {
                chat(markAsError("No model selected"))
                return currentRotation
            }

        ModuleDebug.debugParameter(this, "DeltaYaw", totalDelta.deltaYaw)
        ModuleDebug.debugParameter(this, "DeltaPitch", totalDelta.deltaPitch)

        val input = TrainingData(
            currentVector = currentRotation.directionVector,
            previousVector = prevRotation.directionVector,
            targetVector = targetRotation.directionVector,
            velocityDelta = velocityDelta.toVec2f(),
            entityDistance = entityDistance,
            entityDistanceDelta = entityDistance - previousEntityDistance,
            age = min(MAXIMUM_TRAINING_AGE, RotationManager.ticksSinceChange)
        )

        previousEntityDistance = entityDistance

        val output = model.predictor.predict(input.asInput)
        ModuleDebug.debugParameter(this, "Output [0]", output[0])
        ModuleDebug.debugParameter(this, "Output [1]", output[1])

        return Rotation(
            currentRotation.yaw + output[0] * outputMultiplier.yawMultiplier,
            currentRotation.pitch + output[1] * outputMultiplier.pitchMultiplier
        )
    }

    override fun howLongToReach(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        return 0
    }

}
