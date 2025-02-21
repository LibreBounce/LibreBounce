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
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.min

/**
 * Record using
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiCombatRecorder]
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiTrainer]
 * and then train a model - after that you will be able to use it with
 * [net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth.MinaraiSmoothMode].
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

    data class NoneAngleSmoothMode(override val parent: ChoiceConfigurable<*>) : AngleSmoothMode("None") {

        override fun limitAngleChange(
            rotationFactor: Float,
            currentRotation: Rotation,
            targetRotation: Rotation,
            vec3d: Vec3d?,
            entity: Entity?
        ) = currentRotation

        override fun howLongToReach(
            currentRotation: Rotation,
            targetRotation: Rotation
        ): Int = 0

    }

    private var correctionMode = choices(this, "Correction") {
        arrayOf(
            NoneAngleSmoothMode(it),
            LinearAngleSmoothMode(it),
            SigmoidAngleSmoothMode(it)
        )
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

        val entity = entity as? LivingEntity
        val inputModelRotation = targetRotation
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val totalDelta = currentRotation.rotationDeltaTo(inputModelRotation)
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
            targetVector = inputModelRotation.directionVector,
            velocityDelta = velocityDelta.toVec2f(),

            playerDiff = player.pos.subtract(player.prevPos),
            targetDiff = entity?.let { entity.pos.subtract(entity.prevPos) } ?: Vec3d.ZERO,

            hurtTime = entity?.let {entity.hurtTime } ?: 10,
            distance = entity?.let { player.squaredBoxedDistanceTo(entity).toFloat() } ?: 3f,
            age = min(MAXIMUM_TRAINING_AGE, RotationManager.ticksSinceChange)
        )

        val output = model.predictor.predict(input.asInput)
        ModuleDebug.debugParameter(this, "Output [0]", output[0])
        ModuleDebug.debugParameter(this, "Output [1]", output[1])

        val modelOutput = Rotation(
            currentRotation.yaw + output[0] * outputMultiplier.yawMultiplier,
            currentRotation.pitch + output[1] * outputMultiplier.pitchMultiplier
        )

        return correctionMode.activeChoice.limitAngleChange(
            rotationFactor,
            modelOutput,
            inputModelRotation,
            vec3d,
            entity
        )
    }

    override fun howLongToReach(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        // TODO: Implement correctly
        return 0
    }

}
