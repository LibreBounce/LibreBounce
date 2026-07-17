/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position
import net.minecraft.network.packet.s2c.play.PlayerMoveS2CPacket

object Cancel : NoFallMode("Cancel") {
    /**
     * NoFall Cancel
     * NOTE: The recommended distance for falling is < 15.
     */
    private var isFalling = false

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (event.isCancelled)
            return

        if (packet is PlayerMoveS2CPacket && isFalling) {
            sendPacket(Position(packet.x, packet.y, packet.z, true))
            isFalling = false
        }

        if (packet is PlayerMoveC2SPacket) {
            if (mc.player.fallDistance > 3F) {
                isFalling = true
                event.cancelEvent()
            }
        }
    }

    override fun onDisable() {
        mc.player.fallDistance = 0F
        isFalling = false
    }
}