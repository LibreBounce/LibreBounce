/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Flag : FlyMode("Flag") {
    override fun onUpdate() {
        mc.thePlayer?.run { player ->
            val (x, y, z) = player

            sendPackets(
                C04PacketPlayerPosition(
                    x + motionX * 999,
                    y + (if (mc.gameSettings.keyBindJump.isKeyDown) 1.5624 else 0.00000001) - if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0624 else 0.00000002,
                    z + motionZ * 999,
                    true
                ),
                C04PacketPlayerPosition(
                    x + motionX * 999,
                    y - 6969,
                    z + motionZ * 999,
                    true
                )
            )

            setPosition(x + motionX * 11, y, z + motionZ * 11)
            motionY = 0.0
        }
    }
}
