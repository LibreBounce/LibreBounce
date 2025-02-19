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
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import java.util.Date
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.toTypedArray
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

/**
 * Records acceleration behavior
 */
object MinaraiRecorder : ModuleDebugRecorder.DebugRecorderMode<TrainingData>("Minarai") {

    private var model: MinaraiModel? = null
    private var successfulHit = false

    const val MAXIMUM_SAMPLES = 15

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val slime = spawn()

        val previous = ThreadLocal.withInitial { RotationManager.currentRotation ?: player.rotation }
        val inbetweens = ArrayDeque<Inbetween>(MAXIMUM_SAMPLES)

        // Wait until we start collecting data
        var inbetween: Inbetween? = null
        val timeWithoutMovement = waitUntil {
            inbetween = collectInbetween(slime, previous)

            !MathHelper.approximatelyEquals(0.0f, inbetween.delta.length())
        }
        inbetweens.add(inbetween!!)

        val timeWithMovement = waitUntil {
            val prevSnapshot = inbetweens.last()
            val snapshot = collectInbetween(slime, previous)
            inbetweens.addLast(snapshot)
            if (inbetweens.size > 15) {
                inbetweens.removeFirst()
            }

            successfulHit && !MathHelper.approximatelyEquals(0.0f, inbetween.delta.length())
        }

        // Check if in-betweens is higher than

        // Last snapshot is the target vector
        val target = inbetweens.last().current

        // Remap data to [TrainingData] after we collected target vector
        var prevInbetween: Inbetween? = null
        for (snapshot in inbetweens) {
            recordPacket(TrainingData(
                currentVector = snapshot.current,
                previousVector = snapshot.previous,
                targetVector = target,
                velocityDelta = snapshot.delta,
                entityDistance = snapshot.distance,
                entityDistanceDelta = snapshot.distance - (prevInbetween?.distance ?: snapshot.distance),
                age = snapshot.age
            ))

            prevInbetween = snapshot
        }

        successfulHit = false
        world.removeEntity(slime.id, Entity.RemovalReason.DISCARDED)
        chat("✧ Recorded ${inbetweens.size} samples")
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
            player.yaw + Random.nextDouble(-45.0, 45.0).toFloat(),
            Random.nextDouble(-2.0, 3.0).toFloat()
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
        val data = packets.mapNotNull(TrainingData::map)
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

    /**
     * Collects data for the current tick
     */
    private fun collectInbetween(slimeEntity: SlimeEntity, previous: ThreadLocal<Rotation>): Inbetween {
        val next = RotationManager.currentRotation ?: player.rotation
        val current = RotationManager.previousRotation ?: player.lastRotation
        val previous = previous.get().apply {
            previous.set(current)
        }

        return Inbetween(
            current = current.directionVector,
            previous = previous.directionVector,
            delta = current.rotationDeltaTo(next).toVec2f(),
            distance = player.squaredBoxedDistanceTo(slimeEntity).toFloat(),
            age = slimeEntity.age
        )
    }

    private data class Inbetween(
        val current: Vec3d,
        val previous: Vec3d,
        val delta: Vec2f,
        val distance: Float,
        val age: Int
    )

}
