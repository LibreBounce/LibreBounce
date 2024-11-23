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
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.aiming.NoRotationMode
import net.ccbluex.liquidbounce.utils.aiming.NormalRotationMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.getExplosionDamageFromEntity
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket

class CrystalDestroyFeature(listenable: Listenable, private val module: Module) :
    ToggleableConfigurable(listenable, "DestroyCrystals", true) {

    private val range by float("Range", 4.5f, 1f..6f)
    private val wallRange by float("WallRange", 4.5f, 0f..6f)
    private val delay by int("Delay", 0, 0..1000, "ms")
    private val swing by boolean("Swing", true)

    val rotationMode = choices<RotationMode>(this, "RotationMode", { it.choices[0] }, {
        arrayOf(NormalRotationMode(it, module, Priority.IMPORTANT_FOR_USAGE_3), NoRotationMode(it, module))
    })

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
        val target = currentTarget ?: return@repeatable

        if (!chronometer.hasElapsed(delay.toLong())) {
            return@repeatable
        }

        if (wouldKill(target)) {
            currentTarget = null
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

        rotationMode.activeChoice.rotate(rotation, isFinished = {
            facingEnemy(
                toEntity = target,
                rotation = RotationManager.serverRotation,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble()
            )
        }, onFinished = {
            if (!chronometer.hasElapsed(delay.toLong())) {
                return@rotate
            }

            val target1 = currentTarget ?: return@rotate

            if (wouldKill(target1)) {
                currentTarget = null
                return@rotate
            }

            target1.attack(swing)
            chronometer.reset()
            currentTarget = null
        })
    }

    /**
     * Checks whether the crystal would kill us.
     */
    private fun wouldKill(target: EndCrystalEntity): Boolean {
        val health = player.health + player.absorptionAmount
        return health - player.getExplosionDamageFromEntity(target) <= 0f
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
     * This should be called when the module using this destroyer is disabled.
     */
    fun onDisable() {
        currentTarget = null
    }

}
