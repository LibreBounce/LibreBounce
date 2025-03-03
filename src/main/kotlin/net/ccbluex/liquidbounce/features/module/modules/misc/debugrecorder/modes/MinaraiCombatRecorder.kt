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

import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny

/**
 * Records combat behavior
 */
object MinaraiCombatRecorder : ModuleDebugRecorder.DebugRecorderMode<TrainingData>("MinaraiCombat") {

    private val recordWhenClicking by boolean("WhenClicking", true)

    private var targetTracker = TargetTracker()
    private var previous: Rotation = Rotation(0f, 0f)

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (player.abilities.allowFlying || player.isSpectator || player.isDead || player.abilities.flying) {
            return@tickHandler
        }

        if (recordWhenClicking && !mc.options.attackKey.isPressedOnAny) {
            return@tickHandler
        }

        val next = RotationManager.currentRotation ?: player.rotation
        val current = RotationManager.previousRotation ?: player.lastRotation
        val previous = previous.apply {
            previous = current
        }

        val target = targetTracker.selectFirst() ?: return@tickHandler

        recordPacket(
            TrainingData(
                currentVector = current.directionVector,
                previousVector = previous.directionVector,
                targetVector = Rotation.lookingAt(point = target.eyePos, from = player.eyePos).directionVector,
                velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                playerDiff = player.pos.subtract(player.prevPos),
                targetDiff = target.pos.subtract(target.prevPos),
                age = target.age,
                hurtTime = target.hurtTime,
                distance = player.squaredBoxedDistanceTo(target).toFloat()
            )
        )

        mc.inGameHud.setOverlayMessage(("${packets.size} samples recorded, currently tracking: " +
            "${target.nameForScoreboard}").asText(), false)
    }

}
