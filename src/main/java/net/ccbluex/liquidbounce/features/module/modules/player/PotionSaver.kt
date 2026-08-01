/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object PotionSaver : Module("PotionSaver", Category.PLAYER) {

    val onPacket = handler<PacketEvent> {
        mc.player?.run {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket && !isUsingItem && !packet.rotating &&
                (!packet.hasPos || (packet.x == prevX && packet.y == prevY && packet.z == prevZ))
            )
                it.cancelEvent()
        }
    }

}