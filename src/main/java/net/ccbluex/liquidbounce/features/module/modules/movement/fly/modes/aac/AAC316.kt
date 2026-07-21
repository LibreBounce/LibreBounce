/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object AAC316 : FlyMode("AAC3.1.6-Gomme") {
    private var tick = 0
    private var noFlag = false

    override fun onUpdate() {
        mc.player?.run {
            abilities.flying = true

            if (tick == 2) {
                motionY += 0.05
            } else if (tick > 2) {
                motionY -= 0.05
                tick = 0
            }

            tick++

            if (!noFlag)
                sendPacket(
                    Position(posX, posY, posZ, onGround)
                )

            if (posY <= 0.0) noFlag = true
        }
    }

    override fun onDisable() {
        noFlag = false
    }
}
