/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object Flag : FlyMode("Flag") {
    override fun onUpdate() {
        mc.thePlayer?.run {
            sendPackets(
                Position(
                    posX + motionX * 999,
                    posY + (if (mc.gameSettings.keyBindJump.isKeyDown) 1.5624 else 0.00000001) - if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0624 else 0.00000002,
                    posZ + motionZ * 999,
                    true
                ),

                Position(
                    posX + motionX * 999,
                    posY - 6969,
                    posZ + motionZ * 999,
                    true
                )
            )

            setPosition(posX + motionX * 11, posY, posZ + motionZ * 11)
            motionY = 0.0
        }
    }
}
