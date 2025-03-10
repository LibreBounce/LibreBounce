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

    // We assume the velocity has reset.
    private inline val hasVelocityReset
        get() =
            abs(player.velocity.x) == 0.0 &&
                abs(player.velocity.y) == 0.0 &&
                abs(player.velocity.z) == 0.0

    init {
        sequenceHandler<PacketEvent> { event ->
            val packet = event.packet

            if (
                (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) ||
                    packet is ExplosionS2CPacket && 
                    (requiresKillaura && !ModuleKillAura.running)
            ) {
                reset()
                handlingVelocity = true
            }
        }

        sequenceHandler<PlayerTickEvent> {
            if (!handlingVelocity) return@sequenceHandler
            if (hasVelocityReset) reset()

            if (velocityTicks++ >= delay) {
                player.velocity.x *= -xModifier
                player.velocity.z *= -zModifier
                reset()
            }
        }
    }

    private fun reset() {
        velocityTicks = 0
        handlingVelocity = false
    }
}
