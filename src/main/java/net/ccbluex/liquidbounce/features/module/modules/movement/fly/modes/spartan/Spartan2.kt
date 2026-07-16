/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object Spartan2 : FlyMode("Spartan2") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            strafe(0.264f)

            if (ticksExisted % 8 == 0)
                sendPacket(Position(posX, posY + 10, posZ, true))
        }
    }
}
