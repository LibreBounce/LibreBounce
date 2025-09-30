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
import net.minecraft.network.play.client.C03PacketPlayer

object PotionSaver : Module("PotionSaver", Category.PLAYER) {

    val onPacket = handler<PacketEvent> {
        val player = mc.thePlayer ?: return@handler
        val packet = it.packet

        if (packet is C03PacketPlayer && player?.isUsingItem == false && !packet.rotating &&
            (!packet.isMoving || (packet.x == player.lastTickPosX && packet.y == player.lastTickPosY && packet.z == player.lastTickPosZ))
        )
            it.cancelEvent()
    }

}