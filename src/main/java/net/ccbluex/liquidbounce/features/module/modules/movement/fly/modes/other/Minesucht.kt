/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object Minesucht : FlyMode("Minesucht") {
    private var minesuchtTP = 0L

    override fun onUpdate() {
        mc.player?.run {
            if (!mc.options.forwardKey.isPressed) return

            if (System.currentTimeMillis() - minesuchtTP > 99) {
                val vec = eyes + getRotationVec(1f) * 7.0

                if (fallDistance > 0.8) {
                    sendPackets(
                        Position(x, y + 50, z, false),
                        Position(x, y + 20, z, true)
                    )
                    fall(100f, 100f)
                    fallDistance = 0f
                }
                sendPackets(
                    Position(vec.xCoord, y + 50, vec.zCoord, true),
                    Position(x, y, z, false),
                    Position(vec.xCoord, y, vec.zCoord, true),
                    Position(x, y, z, false)
                )
                minesuchtTP = System.currentTimeMillis()
            } else {
                sendPackets(
                    Position(x, y, z, false),
                    Position(x, y, z, true)
                )
            }
        }
    }
}