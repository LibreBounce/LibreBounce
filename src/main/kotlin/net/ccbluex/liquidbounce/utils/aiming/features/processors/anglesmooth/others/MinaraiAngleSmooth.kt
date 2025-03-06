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

package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.others

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelHolster.models
import net.ccbluex.liquidbounce.deeplearn.data.MAXIMUM_TRAINING_AGE
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.functions.LinearAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.functions.NoneAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.functions.SigmoidAngleSmooth
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.min
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/**
 * Record using
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiCombatRecorder]
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiTrainer]
 * and then train a model - after that you will be able to use it with
 * [net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.others.MinaraiAngleSmooth].
 */
class MinaraiAngleSmooth(override val parent: ChoiceConfigurable<*>) : AngleSmooth("Minarai") {

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

    private var correctionMode = choices(this, "Correction") {
        arrayOf(
            NoneAngleSmooth(it),
            LinearAngleSmooth(it),
            SigmoidAngleSmooth(it),
            AccelerationAngleSmooth(it)
        )
    }

    private val outputMultiplier = tree(OutputMultiplier())

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        if (!DeepLearningEngine.isInitialized) {
            chat(markAsError("No deep learning engine found."))
            return currentRotation
        }

        val entity = rotationTarget.entity as? LivingEntity
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val totalDelta = currentRotation.rotationDeltaTo(targetRotation)
        val velocityDelta = prevRotation.rotationDeltaTo(currentRotation)

        ModuleDebug.debugParameter(this, "DeltaYaw", totalDelta.deltaYaw)
        ModuleDebug.debugParameter(this, "DeltaPitch", totalDelta.deltaPitch)

        val input = TrainingData(
            currentVector = currentRotation.directionVector,
            previousVector = prevRotation.directionVector,
            targetVector = targetRotation.directionVector,
            velocityDelta = velocityDelta.toVec2f(),

            playerDiff = player.pos.subtract(player.prevPos),
            targetDiff = entity?.let { entity.pos.subtract(entity.prevPos) } ?: Vec3d.ZERO,

            hurtTime = entity?.let {entity.hurtTime } ?: 10,
            distance = entity?.let { player.squaredBoxedDistanceTo(entity).toFloat() } ?: 3f,
            age = min(MAXIMUM_TRAINING_AGE, RotationManager.reactionTicks)
        )

        val (output, time) = measureTimedValue {
            choices.activeChoice.predict(input.asInput)
        }
        ModuleDebug.debugParameter(this, "Output [0]", output[0])
        ModuleDebug.debugParameter(this, "Output [1]", output[1])
        ModuleDebug.debugParameter(this, "Time", "${time.toString(DurationUnit.MILLISECONDS, 2)} ms")

        val modelOutput = Rotation(
            currentRotation.yaw + output[0] * outputMultiplier.yawMultiplier,
            currentRotation.pitch + output[1] * outputMultiplier.pitchMultiplier
        )

        return correctionMode.activeChoice.process(
            rotationTarget,
            modelOutput,
            targetRotation
        )
    }

    override fun calculateTicks(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        // TODO: Implement correctly
        return 0
    }

}
