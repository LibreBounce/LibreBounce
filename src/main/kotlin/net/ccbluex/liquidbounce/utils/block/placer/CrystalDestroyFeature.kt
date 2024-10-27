/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
 */
package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceBox
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket

class CrystalDestroyFeature(listenable: Listenable, private val module: Module) :
    ToggleableConfigurable(listenable, "DestroyCrystals", true) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val wallRange by float("WallRange", 4.5f, 0f..6f)
    private val delay by int("Delay", 0, 0..1000, "ms")
    private val swing by boolean("Swing", true)

    private class Rotate(parent: Listenable) : ToggleableConfigurable(parent, "Rotate", true) {
        val rotations = tree(RotationsConfigurable(this))
    }

    private val rotate = tree(Rotate(this))

    private val chronometer = Chronometer()

    var currentTarget: EndCrystalEntity? = null
        set(value) {
            if (value != field && value != null) {
                raytraceBox(
                    player.eyePos,
                    value.boundingBox,
                    range = range.toDouble(),
                    wallsRange = wallRange.toDouble(),
                )?.let { field = value }
            } else {
                field = value
            }
        }

    val repeatable = repeatable {
        if (!rotate.enabled) {
            return@repeatable
        }

        val target = currentTarget ?: return@repeatable

        if (!chronometer.hasElapsed(delay.toLong())) {
            return@repeatable
        }

        // find the best spot (and skip if no spot was found)
        val (rotation, _) =
            raytraceBox(
                player.eyePos,
                target.boundingBox,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble(),
            ) ?: return@repeatable

        // aim at the target
        RotationManager.aimAt(
            rotation,
            configurable = rotate.rotations,
            priority = Priority.IMPORTANT_FOR_USAGE_3,
            provider = module
        )
    }

    @Suppress("unused")
    val postRotateHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state == EventState.PRE) {
            // rotating is already handled by the rotation manager
            return@handler
        }

        val target = currentTarget ?: return@handler

        if (!chronometer.hasElapsed(delay.toLong())) {
            return@handler
        }

        if (rotate.enabled && !facingEnemy(
                toEntity = target,
                rotation = RotationManager.serverRotation,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble()
            )) {
            return@handler
        }

        target.attack(swing)
        chronometer.reset()
        currentTarget = null
    }

    @Suppress("unused")
    val destroyEntityHandler = handler<PacketEvent> {
        val target = currentTarget ?: return@handler

        val packet = it.packet
        if (packet is EntitiesDestroyS2CPacket && target.id in packet.entityIds) {
            currentTarget = null
        }
    }

    /**
     * THis should be called when the module using this destroyer is disabled.
     */
    fun onDisable() {
        currentTarget = null
    }

}
