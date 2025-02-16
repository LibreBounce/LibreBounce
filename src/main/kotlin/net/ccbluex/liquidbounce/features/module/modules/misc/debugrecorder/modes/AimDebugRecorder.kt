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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.gson.util.json
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult

object AimDebugRecorder : ModuleDebugRecorder.DebugRecorderMode("Aim") {

    private val targetTracker = TargetTracker(TargetPriority.DIRECTION, FloatValueProvider("Range", 7f, 4f..10f))

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val playerRotation = player.rotation
        val playerLastRotation = player.lastRotation
        val target = targetTracker.selectFirst() ?: return@tickHandler
        val turnSpeed = playerLastRotation.rotationDeltaTo(playerRotation)

        recordPacket(JsonObject().apply {
            val cDirVec = player.rotation.directionVector
            add("c_vector", json {
                "x" to cDirVec.x
                "y" to cDirVec.y
                "z" to cDirVec.z
            })
            val rotation = Rotation.lookingAt(point = target.eyePos, from = player.eyePos)
            val wDirVec = rotation.directionVector
            add("w_vector", json {
                "x" to wDirVec.x
                "y" to wDirVec.y
                "z" to wDirVec.z
            })
            add("delta", json {
                "x" to turnSpeed.deltaYaw
                "y" to turnSpeed.deltaPitch
            })
            addProperty("distance", player.squaredBoxedDistanceTo(target))

            val crosshairTarget = mc.crosshairTarget
            addProperty("reward",
                if (crosshairTarget?.type == HitResult.Type.ENTITY && crosshairTarget is EntityHitResult) {
                    crosshairTarget.entity.id == target.id
                } else {
                    false
                }
            )
        })
    }

}
