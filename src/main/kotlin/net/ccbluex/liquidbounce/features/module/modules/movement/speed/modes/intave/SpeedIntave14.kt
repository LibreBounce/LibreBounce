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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.intave

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.directionYaw
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.entity.MovementType

/**
 * Intave 14 speed
 *
 * @author larryngton
 */
class SpeedIntave14(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Intave14", parent) {


    companion object {
        private const val BOOST_CONSTANT = 0.003
    }

    private inner class Strafe(parent: EventListener) : ToggleableConfigurable(parent, "Strafe", true) {

        private val strength by float("Strength", 0.29f, 0.01f..0.29f)

        @Suppress("unused")
        private val moveHandler = handler<PlayerMoveEvent> { event ->
            if (event.type == MovementType.SELF && player.isOnGround && player.isSprinting) {
                event.movement.strafe(
                    player.directionYaw,
                    strength = strength.toDouble()
                )
            }
        }
    }

    private inner class AirBoost(parent: EventListener) : ToggleableConfigurable(parent, "AirBoost", true) {

        private val initialBoostMultiplier by float(
            "InitialBoostMultiplier", 1f,
            0.01f..10f
        )

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.velocity.y > 0.003 && player.isSprinting) {
                player.velocity.x *= 1f + (BOOST_CONSTANT * initialBoostMultiplier.toDouble())
                player.velocity.z *= 1f + (BOOST_CONSTANT * initialBoostMultiplier.toDouble())
            }
        }
    }

    init {
        tree(Strafe(this))
        tree(AirBoost(this))
    }

    /**
     * Does not affect much, but we take what we can get.
     */
    private val lowHop by boolean("LowHop", true)

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        if (lowHop) {
            event.motion = 0.42f - 1.7E-14f
        }
    }

}
