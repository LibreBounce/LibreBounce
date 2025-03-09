package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import kotlin.math.abs

internal object VelocityReversal : VelocityMode("Reversal") {
    private val delay by int("Delay", 2, 1..5, "ticks")
    private val requiresKillaura by boolean("RequiresKillAura", true)
    private val xModifier by float("XModifier", 0.5f, 0.1f..1.0f)
    private val zModifier by float("ZModifier", 0.5f, 0.1f..1.0f)

    private var handlingVelocity = false
    private var velocityTicks = 0

    init {
        sequenceHandler<PacketEvent> { event ->
            val packet = event.packet

            if ((packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) || packet is ExplosionS2CPacket) {
                if (requiresKillaura && !ModuleKillAura.running) return@sequenceHandler
                reset()
                handlingVelocity = true
            }
        }

        sequenceHandler<PlayerTickEvent> {
            if (handlingVelocity) {
                if (velocityTicks++ >= delay) {
                    player.velocity.x *= -xModifier
                    player.velocity.z *= -zModifier
                    reset()
                }

                // We assume the velocity has reset
                // Idk of any edge cases where this logic would fail.
                if (abs(player.velocity.x) == 0.0 &&
                    abs(player.velocity.y) == 0.0 &&
                    abs(player.velocity.z) == 0.0) {
                    reset()
                }
            }
        }
    }

    private fun reset() {
        velocityTicks = 0
        handlingVelocity = false
    }
}
