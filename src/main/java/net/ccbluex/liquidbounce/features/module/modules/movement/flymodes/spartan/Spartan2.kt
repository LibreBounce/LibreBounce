/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Spartan2 : FlyMode("Spartan2") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            strafe(0.264f)

            if (ticksExisted % 8 == 0)
                sendPacket(C04PacketPlayerPosition(posX, posY + 10, posZ, true))
        }
    }
}
