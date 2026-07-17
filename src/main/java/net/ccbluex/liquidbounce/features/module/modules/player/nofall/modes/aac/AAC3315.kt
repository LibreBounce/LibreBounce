/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object AAC3315 : NoFallMode("AAC3.3.15") {
    override fun onUpdate() {
        mc.player?.run {
            if (mc.isIntegratedServerRunning) return

            if (fallDistance > 2) {
                sendPacket(Position(posX, Double.NaN, posZ, false))

                fallDistance = -9999f
            }
        }
    }
}