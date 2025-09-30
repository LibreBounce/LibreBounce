/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.ncpMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object NCP : FlyMode("NCP") {
    override fun onEnable() {
        mc.thePlayer?.run {
            if (!onGround) return

            repeat(65) {
                sendPackets(
                    C04PacketPlayerPosition(posX, posY + 0.049, posZ, false),
                    C04PacketPlayerPosition(posX, posY, posZ, false)
                )
            }

            sendPacket(C04PacketPlayerPosition(posX, posY + 0.1, posZ, true))

            motionX *= 0.1
            motionZ *= 0.1
            swingItem()
        }
    }

    override fun onUpdate() {
        mc.thePlayer.motionY =
            if (mc.gameSettings.keyBindSneak.isKeyDown) -0.5
            else -ncpMotion.toDouble()

        strafe()
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is C03PacketPlayer)
            event.packet.onGround = true
    }

}
