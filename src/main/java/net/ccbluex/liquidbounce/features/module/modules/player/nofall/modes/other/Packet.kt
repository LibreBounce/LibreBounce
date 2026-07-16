/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.other

import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object Packet : NoFallMode("Packet") {
    override fun onUpdate() {
        if (mc.thePlayer.fallDistance > 2f)
            sendPacket(PlayerMoveC2SPacket(true))
    }
}