package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoFallPacketJump : Choice("PacketJump") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL,
        arrayOf(MovePacketType.FULL, MovePacketType.POSITION_AND_ON_GROUND))
    private val onLanding by boolean("OnLanding", true)
    private val minDistance by float("MinDistance", 3f, 0f..3f)

    private var falling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val tickHandler = handler<PlayerTickEvent> {
        falling = player.fallDistance > minDistance
        if (!onLanding && !player.isOnGround && falling) {
            network.sendPacket(packetType.generatePacket().apply {
                y += 1.0E-9
            })
            player.onLanding()
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (onLanding && event.packet is PlayerMoveC2SPacket && event.packet.onGround && falling) {
            falling = false
            network.sendPacket(packetType.generatePacket().apply {
                x = player.lastX
                y = player.lastBaseY + 1.0E-9
                z = player.lastZ
                onGround = false
            })
            player.onLanding()
        }
    }
}
