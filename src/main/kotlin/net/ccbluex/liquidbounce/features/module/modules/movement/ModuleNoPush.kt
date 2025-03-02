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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * NoPush module
 *
 * Disables pushing from other players and some other situations where someone/something can push.
 */
object ModuleNoPush : ClientModule("NoPush", Category.MOVEMENT) {

    private val entities by boolean("Entities", true)
    private val blocks by boolean("Blocks", true)
    private val fishingRod by boolean("FishingRod", false)
    private val liquids by boolean("Liquids", false)
    private val sinking by boolean("Sinking", false)

    fun isLiquids() = running && liquids

    fun isEntities() = running && entities

    fun isBlocks() = running && blocks

    fun isFishingRod() = running && fishingRod

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!sinking) {
            return@handler
        }

        if (mc.options.jumpKey.isPressed || mc.options.sneakKey.isPressed) {
            return@handler
        }

        if ((player.isTouchingWater || player.isInLava) && player.velocity.y < 0) {
            player.velocity.y = 0.0
        }
    }

}
