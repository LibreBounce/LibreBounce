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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import kotlin.math.abs
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * A velocity mode that reverses your velocity after a set amount of ticks.
 * Default values bypass Vulcan (3/9/25) ~ anticheat-test.com
 */
internal object VelocityReversal : VelocityMode("Reversal") {
    private val delay by int("Reversal Delay", 2, 1..5, "ticks")
    private val xModifier by float("X Modifier", 0.5f, 0.1f..1.0f)
    private val zModifier by float("Z Modifier", 0.5f, 0.1f..1.0f)
    private val requiresKillaura by boolean("RequiresKillAura", true)

    private var handlingVelocity = false
    private var velocityTicks = 0

    // We assume the velocity has reset. Idk of any edge cases where this logic would fail.
    private inline val hasVelocityReset
        get() =
            abs(player.velocity.x) == 0.0 &&
                abs(player.velocity.y) == 0.0 &&
                abs(player.velocity.z) == 0.0

    // TODO: Yes, type: any check. I'll fix this when I have the time.
    private fun checkRequirements(packet: Any): Boolean {
        val isKillAuraRunning = requiresKillaura && !ModuleKillAura.running
        val isExplosion = packet is ExplosionS2CPacket
        val isSelfVelocity = packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id
        return (isSelfVelocity || isExplosion) && isKillAuraRunning
    }

    @Supress("unused")
    private val packetEventHandler =
        sequenceHandler<PacketEvent> { event ->
            if (!checkRequirements(event.packet)) return@sequenceHandler

            reset()
            handlingVelocity = true
        }

    @Supress("unused")
    private val playerTickHandler =
        sequenceHandler<PlayerTickEvent> {
            if (!handlingVelocity) return@sequenceHandler
            if (hasVelocityReset) reset()

            if (velocityTicks++ >= delay) {
                player.velocity.x *= -xModifier
                player.velocity.z *= -zModifier
                reset()
            }
        }

    private fun reset() {
        velocityTicks = 0
        handlingVelocity = false
    }
}
