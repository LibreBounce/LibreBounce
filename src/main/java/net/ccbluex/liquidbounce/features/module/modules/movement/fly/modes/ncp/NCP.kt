/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.ncpMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object NCP : FlyMode("NCP") {
    override fun onEnable() {
        mc.player?.run {
            if (!onGround) return

            repeat(65) {
                sendPackets(
                    Position(x, y + 0.049, z, false),
                    Position(x, y, z, false)
                )
            }

            sendPacket(Position(x, y + 0.1, z, true))

            velocityX *= 0.1
            velocityZ *= 0.1
            swingArm()
        }
    }

    override fun onUpdate() {
        mc.player.velocityY =
            if (mc.options.sneakKey.isPressed) -0.5
            else -ncpMotion.toDouble()

        strafe()
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket)
            event.packet.onGround = true
    }

}
