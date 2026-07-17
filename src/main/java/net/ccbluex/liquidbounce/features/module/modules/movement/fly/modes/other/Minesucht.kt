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
            if (!mc.gameSettings.keyBindForward.isKeyDown) return

            if (System.currentTimeMillis() - minesuchtTP > 99) {
                val vec = eyes + getLook(1f) * 7.0

                if (fallDistance > 0.8) {
                    sendPackets(
                        Position(posX, posY + 50, posZ, false),
                        Position(posX, posY + 20, posZ, true)
                    )
                    fall(100f, 100f)
                    fallDistance = 0f
                }
                sendPackets(
                    Position(vec.xCoord, posY + 50, vec.zCoord, true),
                    Position(posX, posY, posZ, false),
                    Position(vec.xCoord, posY, vec.zCoord, true),
                    Position(posX, posY, posZ, false)
                )
                minesuchtTP = System.currentTimeMillis()
            } else {
                sendPackets(
                    Position(posX, posY, posZ, false),
                    Position(posX, posY, posZ, true)
                )
            }
        }
    }
}