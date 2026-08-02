/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.ncp

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.startY
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object OldNCP : FlyMode("OldNCP") {
    override fun onEnable() {
        mc.player?.run {
            if (!onGround) return

            repeat(4) {
                sendPackets(
                    Position(x, y + 1.01, z, false),
                    Position(x, y, z, false)
                )
            }

            tryJump()
            swingArm()
        }
    }

    override fun onUpdate() {
        mc.player?.run {
            if (startY > y)
                velocityY = -0.000000000000000000000000000000001

            if (mc.options.sneakKey.isPressed)
                velocityY = -0.2

            if (mc.options.jumpKey.isPressed && y < startY - 0.1)
                velocityY = 0.2

            strafe()
        }
    }
}
