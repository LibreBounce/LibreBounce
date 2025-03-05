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

import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectArrayMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.*
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.math.MathHelper

/**
 * Records combat behavior
 */
object MinaraiCombatRecorder : ModuleDebugRecorder.DebugRecorderMode<TrainingData>("MinaraiCombat") {

    private var targetTracker = tree(TargetTracker(
        // Start tracking target that we look at the closest
        defaultPriority = TargetPriority.DIRECTION,

        // Start tracking when 10 blocks away
        rangeValue =  FloatValueProvider("Range", 10f, 7f..12f)
    ))
    private var previous: Rotation = Rotation(0f, 0f)

    private var startMap = Int2ObjectArrayMap<Start>()
    private var trainingCollection = Int2ObjectArrayMap<MutableList<TrainingData>>()
    private var sequenceLocks = IntOpenHashSet()

    private data class Start(
        var age: Int,
        val rotation: Rotation
    )

    const val BUFFER_SIZE = 100

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.abilities.allowFlying || player.isSpectator ||
            player.isDead || player.abilities.flying) {
            return@tickHandler
        }

        if (interaction.isBreakingBlock || player.isUsingItem && !player.isBlockAction) {
            reset()
            return@tickHandler
        }

        val next = RotationManager.currentRotation ?: player.rotation
        val current = RotationManager.previousRotation ?: player.lastRotation
        val previous = previous.apply {
            previous = current
        }
        val targets = targetTracker.targets()

        for (target in targets) {
            val starting = startMap.computeIfAbsent(target.id) { Start(target.age, current) }
            val buffer = trainingCollection.computeIfAbsent(target.id) { mutableListOf() }
            if (buffer.size > BUFFER_SIZE) {
                buffer.removeAt(0)
            }

            buffer.add(TrainingData(
                startingVector = starting.rotation.directionVector,
                currentVector = current.directionVector,
                previousVector = previous.directionVector,
                targetVector = Rotation.lookingAt(point = target.eyePos, from = player.eyePos).directionVector,
                velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                playerDiff = player.pos.subtract(player.prevPos),
                targetDiff = target.pos.subtract(target.prevPos),
                age = target.age - starting.age,
                hurtTime = target.hurtTime,
                distance = player.squaredBoxedDistanceTo(target).toFloat()
            ))
        }

        // Drop from [startingVector] and [trainingCollection] if target is not present anymore
        val targetIds = targets.map { targetId -> targetId.id }

        startMap.keys.removeIf { it !in targetIds }
        trainingCollection.keys.removeIf { it !in targetIds }
    }

    @Suppress("unused")
    private val attackHandler = sequenceHandler<AttackEntityEvent> { event ->
        val entity = event.entity as? LivingEntity ?: return@sequenceHandler
        val entityId = entity.id
        val start = startMap.remove(entity.id) ?: return@sequenceHandler

        // Lock the sequence to prevent multiple recordings
        if (sequenceLocks.contains(entity.id)) {
            return@sequenceHandler
        }
        sequenceLocks.add(entity.id)

        // Wait until entity is hurt
        waitUntil { entity.hurtTime > 0 }

        // Wait until entity is not hurt or disappeared
        waitUntil { entity.hurtTime == 0 || entity.isRemoved || entity.isDead }

        sequenceLocks.remove(entity.id)

        // Pass training collection to recording
        val buffer = trainingCollection.remove(entity.id) ?: return@sequenceHandler
        buffer.forEach(::recordPacket)

        val totalDrag = start.rotation.rotationDeltaTo(player.rotation)
        mc.inGameHud.setOverlayMessage("Recorded ${buffer.size} samples for ${entity.name.string}".asText(), false)
    }

    @Suppress("unused")
    private val inactivityTracker = tickHandler {
        val ticks = waitUntil {
            val rotation = RotationManager.currentRotation ?: player.rotation
            val current = RotationManager.previousRotation ?: player.lastRotation
            // Wait until the player moves
            !MathHelper.approximatelyEquals(rotation.rotationDeltaTo(current).length(), 0.0f)
        }

        // When inactive more than 20 ticks, clear the data
        if (ticks > 20 && trainingCollection.isNotEmpty()) {
            reset()
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        when (packet) {
            is PlayerInteractBlockC2SPacket -> reset()
        }
    }

    private fun reset() {
        startMap.clear()
        trainingCollection.clear()
    }

}
