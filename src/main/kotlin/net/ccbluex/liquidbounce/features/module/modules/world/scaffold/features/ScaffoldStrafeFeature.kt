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
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.MODEL_STATE
import net.minecraft.entity.effect.StatusEffects

object ScaffoldStrafeFeature : ToggleableConfigurable(ModuleScaffold, "Strafe", false) {

    private val speed by float("Speed", 0.247f, 0.0f..5.0f)
    private val hypixel by boolean("Hypixel", false)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    @Suppress("unused")
    private val movementHandler = handler<PlayerMoveEvent> { event ->
        if (onlyOnGround && !player.isOnGround) {
            return@handler
        }

        event.movement = if (hypixel) {
            val speedEffect = player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: -1

            when (speedEffect) {
                -1 -> event.movement.withStrafe(speed = 0.2055)
                0, 1 -> event.movement.withStrafe(speed = 0.292)
                else -> return@handler
            }
        } else {
            event.movement.withStrafe(speed = speed.toDouble())
        }
    }

    private class Pause(parent: EventListener?) : ToggleableConfigurable(parent, "Pause", false) {

        private val pauseSpeed by float("PauseSpeed", 0.1f, 0.0f..5.0f)
        private val pauseAfter by int("PauseEvery", 4, 2..40)

        @Suppress("unused")
        private val movementHandler = handler<PlayerMoveEvent>(priority = MODEL_STATE) { event ->
            if (onlyOnGround && !player.isOnGround) {
                return@handler
            }

            if (player.age % pauseAfter == 0) {
                event.movement = event.movement.withStrafe(speed = pauseSpeed.toDouble())
            }
        }

    }

    init {
        tree(Pause(this))
    }
}
