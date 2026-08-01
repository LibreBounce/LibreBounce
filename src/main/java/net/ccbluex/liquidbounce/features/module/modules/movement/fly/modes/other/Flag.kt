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
        mc.player?.run {
            sendPackets(
                Position(
                    x + motionX * 999,
                    y + (if (mc.options.jumpKey.isKeyDown) 1.5624 else 0.00000001) - if (mc.options.sneakKey.isKeyDown) 0.0624 else 0.00000002,
                    z + motionZ * 999,
                    true
                ),

                Position(
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
