/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan

import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object BugSpartan : FlyMode("BugSpartan") {
    override fun onEnable() {
        mc.thePlayer?.run {
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
        mc.thePlayer.capabilities.isFlying = false

        mc.thePlayer.motionY = when {
            mc.gameSettings.keyBindJump.isKeyDown -> vanillaSpeed.toDouble()
            mc.gameSettings.keyBindSneak.isKeyDown -> -vanillaSpeed.toDouble()
            else -> 0.0
        }

        strafe(vanillaSpeed, true)
    }
}
