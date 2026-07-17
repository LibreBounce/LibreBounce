/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.serverOnGround
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object AAC3311 : NoFallMode("AAC3.3.11") {
    override fun onUpdate() {
        mc.player?.run {
            if (fallDistance > 2) {
                stopXZ()

                sendPackets(
                    Position(posX, posY - 10E-4, posZ, serverOnGround),
                    PlayerMoveC2SPacket(true)
                )
            }
        }
    }
}