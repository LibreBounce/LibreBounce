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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isBlockBelow
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.towerMode
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe

object ScaffoldTowerHypixel : Choice("Hypixel") {

    override val parent: ChoiceConfigurable<Choice>
        get() = towerMode

    val repeatable = tickHandler {
        if (!mc.options.jumpKey.isPressed || ModuleScaffold.blockCount <= 0 || !isBlockBelow) {
            return@tickHandler
        }

        if (player.x % 1.0 != 0.0 && !player.moving) {
            player.velocity.x = (Math.round(player.x).toDouble() - player.x).coerceAtMost(0.281)
        }

        if (player.airTicks > 14) {
            player.velocity.y -= 0.09
            player.velocity = player.velocity.multiply(
                0.6,
                1.0,
                0.6
            )
            return@tickHandler
        }
        when (player.airTicks % 3) {
            0 -> {
                player.velocity.y = 0.42
                player.velocity = player.velocity.withStrafe(speed = 0.247 - (Math.random() / 100f))
            }
            2 -> player.velocity.y = 1 - (player.y % 1.0)
        }
    }


}
