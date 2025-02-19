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

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import net.ccbluex.liquidbounce.deeplearn.ModelHolster.models
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.deeplearn.models.MinaraiModel
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

/**
 * Records acceleration behavior
 */
object MinaraiTrainer : ModuleDebugRecorder.DebugRecorderMode<TrainingData>("MinaraiTrainer") {

    private var model: MinaraiModel? = null
    private var successfulHit = false

    private var previousEntityDistance: Float? = null

    @Suppress("unused")
    private val tickHandler = tickHandler {
        var previous = RotationManager.currentRotation ?: player.rotation

        successfulHit = false
        val slime = spawn()
        waitUntil {
            val next = RotationManager.currentRotation ?: player.rotation
            val current = RotationManager.previousRotation ?: player.lastRotation
            val previous = previous.apply {
                previous = current
            }
            val distance = player.squaredBoxedDistanceTo(slime).toFloat()

            recordPacket(TrainingData(
                currentVector = current.directionVector,
                previousVector = previous.directionVector,
                targetVector = Rotation.lookingAt(player.eyePos, slime.box.center).directionVector,
                velocityDelta = current.rotationDeltaTo(next).toVec2f(),
//                timeIn = 0,
//                timeOut = 0,
                entityDistance = distance,
                previousEntityDistance = previousEntityDistance ?: distance
            ))
            previousEntityDistance = distance

            successfulHit
        }

        world.removeEntity(slime.id, Entity.RemovalReason.DISCARDED)
        chat("✧ Recorded ${packets.size} samples")
    }

    @Suppress("unused")
    private val attackEntity = handler<AttackEntityEvent> {
        successfulHit = true
    }

    override fun enable() {
        if (isInTraining) {
            chat(markAsError("✘ Already training a model..."))
            ModuleDebugRecorder.enabled = false
            return
        }

        val name = dateFormat.format(Date())
        model = MinaraiModel(name, models)

        super.enable()
    }

    override fun disable() {
        if (isInTraining) {
            return
        }

        thread(block = ::training)
        super.disable()
    }

    private var isInTraining = false

    /**
     * Spawns a slime entity about 2.0 - 3.0 blocks away from the player,
     * in a random direction and at a different height.
     */
    fun spawn(): SlimeEntity {
        val slime = SlimeEntity(EntityType.SLIME, world)
        slime.uuid = UUID.randomUUID()

        val distance = Random.nextDouble() * 0.9 + 2.0

        // Spawn at least in view range of the player
        val direction = Rotation(
            player.yaw + Random.nextDouble(-65.0, 65.0).toFloat(),
            Random.nextDouble(-5.0, 1.0).toFloat()
        ).directionVector * distance

        val position = player.eyePos.add(direction)

        slime.setPosition(position)

        world.addEntity(slime)

        // Play sound at position
        world.playSound(
            position.x,
            position.y,
            position.z,
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.NEUTRAL,
            1f,
            1f,
            false
        )

        return slime
    }

    fun training() = runCatching {
        val data = packets
        val model = model!!

        @Suppress("ArrayInDataClass")
        data class Dataset(val features: Array<FloatArray>, val labels: Array<FloatArray>)

        val (dataset, prepareTime) = measureTimedValue {
            val features = data.map(TrainingData::asInput).toTypedArray()
            val labels = data.map(TrainingData::asOutput).toTypedArray()

            Dataset(features, labels)
        }
        chat(
            regular("✧ Prepared dataset with "),
            variable("${dataset.features.size}"),
            regular(" samples in "),
            variable("${prepareTime.inWholeMilliseconds}ms"),
            dot()
        )

        measureTime {
            chat(
                regular("⚡ Starting training for "),
                variable(model.name),
                regular(" model"),
                dot()
            )

            model.train(dataset.features, dataset.labels)
            model.save()
        }
    }.onFailure { exception ->
        isInTraining = false
        logger.error("Error training model", exception)
        chat(markAsError("✘ Error training model: ${exception.message}"))
    }.onSuccess { trainingTime ->
        chat(
            regular("✔ Model trained and saved successfully."),
            variable("${trainingTime.inWholeSeconds}s"),
            dot()
        )
        this.model = null
        isInTraining = false
    }
}
