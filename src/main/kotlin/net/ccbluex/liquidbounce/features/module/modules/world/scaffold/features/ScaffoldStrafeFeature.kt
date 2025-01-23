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
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.withStrafe

object ScaffoldStrafeFeature : ToggleableConfigurable(ModuleScaffold, "Strafe", false) {
    private val speed by float("Speed", 0.247f, 0.0f..5.0f)
    private val hypixel by boolean("Hypixel", false)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    @Suppress("unused")
    private val strafeHandler = tickHandler {
        if (onlyOnGround && !player.isOnGround) {
            return@tickHandler
        }
        if (hypixel) {
            player.velocity = player.velocity.withStrafe(speed = 0.2055)
            return@tickHandler
        }

        player.velocity = player.velocity.withStrafe(speed = speed.toDouble())
    }

    private class Pause(parent: EventListener?) : ToggleableConfigurable(
        parent, "Pause",
        false
    ) {

        private val pauseSpeed by float("PauseSpeed", 0.1f, 0.0f..5.0f)
        private val pauseAfter by int("PauseEvery", 4, 2..40)

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (onlyOnGround && !player.isOnGround) {
                return@tickHandler
            }

            if (player.age % pauseAfter == 0) {
                player.velocity = player.velocity.withStrafe(speed = pauseSpeed.toDouble())
            }
        }
    }

    init {
        tree(Pause(this))
    }
}
