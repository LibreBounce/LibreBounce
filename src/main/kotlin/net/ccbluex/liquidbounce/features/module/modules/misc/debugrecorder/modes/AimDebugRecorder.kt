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
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult

object AimDebugRecorder : ModuleDebugRecorder.DebugRecorderMode("Aim") {

    private val targetTracker = TargetTracker(TargetPriority.DIRECTION, FloatValueProvider("Range", 7f, 4f..10f))

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // Record only when mouse-button is down, this will filter out bad non-combat data
        if (!mc.options.attackKey.isPressed || player.isDead || player.health <= 0) {
            return@tickHandler
        }

        val playerRotation = RotationManager.currentRotation ?: player.rotation
        val playerLastRotation = RotationManager.previousRotation ?: player.lastRotation
        val target = targetTracker.selectFirst() ?: return@tickHandler
        val delta = playerLastRotation.rotationDeltaTo(playerRotation)

        recordPacket(JsonObject().apply {
            // Player Data
            val cDirVec = playerRotation.directionVector
            add("cv", json {
                "x" to cDirVec.x
                "y" to cDirVec.y
                "z" to cDirVec.z
            })
            chat("${delta.deltaYaw} ${delta.deltaPitch}")


            val velocity = player.velocity
            add("pv", json {
                "x" to velocity.x
                "y" to velocity.y
                "z" to velocity.z
            })
            addProperty("s", player.sqrtSpeed)

            // Target Data
            addProperty("dist", player.squaredBoxedDistanceTo(target))
            val offset = player.pos.subtract(target.pos)
            add("o", json {
                "x" to offset.x
                "y" to offset.y
                "z" to offset.z
            })

            // Rotation Data
            val rotation = Rotation.lookingAt(point = target.eyePos, from = player.eyePos)
            val tDirVec = rotation.directionVector
            add("tv", json {
                "x" to tDirVec.x
                "y" to tDirVec.y
                "z" to tDirVec.z
            })
            add("d", json {
                "x" to delta.deltaYaw
                "y" to delta.deltaPitch
            })

            val crosshairTarget = mc.crosshairTarget
            addProperty("ch",
                if (crosshairTarget?.type == HitResult.Type.ENTITY && crosshairTarget is EntityHitResult) {
                    crosshairTarget.entity.id == target.id
                } else {
                    false
                }
            )
        })
    }

}
