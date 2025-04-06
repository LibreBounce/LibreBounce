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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import kotlin.random.Random

/**
 * Reach module
 *
 * Increases your reach.
 */
object ModuleReach : ClientModule("Reach", Category.PLAYER) {
    object Combat : Configurable("Combat") {
        val reach by floatRange("Reach", 4.2f..4.2f, 3f..8f).onChanged { updateReach(true) }
        private val chance by float("Chance", 100f, 0f..100f, "%")
        private val delay by int("Delay", 0, 0..1000, "ticks")

        private var lastReachTick = 0

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            if (event.packet is PlayerInteractEntityC2SPacket) {
                if (combatReach != null) {
                    lastReachTick = player.age
                }

                updateReach(false)
            }
        }

        @Suppress("unused")
        private val tickHandler = tickHandler {
            if (player.age - lastReachTick == delay) {
                updateReach(false)
            }
        }

        init {
            updateReach(true)
        }

        private fun updateReach(causedBySettingChange: Boolean) {
            if (causedBySettingChange) {
                if (combatReach != null) {
                    combatReach = reach.random()
                }

                return
            }

            combatReach = if (player.age - lastReachTick >= delay && Random.nextFloat() * 100 <= chance) reach.random() else null
        }
    }

    init {
        tree(Combat)
    }

    val blockReach by float("BlockReach", 5f, 4.5f..8f)

    override val tag: String
        get() = if (Combat.reach.start == Combat.reach.endInclusive) {
            "${Combat.reach.start}"
        } else {
            "${Combat.reach.start} - ${Combat.reach.endInclusive}"
        }

    var combatReach: Float? = null
        private set
}
