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
package net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.modes

import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.elytrafly.ModuleElytraFly
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.withStrafe

internal object ElytraFlyModeStatic : ElytraFlyMode("Static") {

    @Suppress("unused")
    private val networkMovementTickHandler = handler<PlayerMoveEvent> { event ->
        if (ModuleElytraFly.shouldNotOperate() || !player.isGliding) {
            return@handler
        }

        val speed = ModuleElytraFly.Speed.enabled
        if (speed && player.moving) {
            event.movement = event.movement.withStrafe(speed = ModuleElytraFly.Speed.horizontal.toDouble())
        } else {
            event.movement.x = 0.0
            event.movement.z = 0.0
        }

        event.movement.y = when {
            mc.options.jumpKey.isPressed && speed -> ModuleElytraFly.Speed.vertical.toDouble()
            mc.options.sneakKey.isPressed && speed -> -ModuleElytraFly.Speed.vertical.toDouble()
            else -> 0.0
        }
    }

}
