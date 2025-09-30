/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Minesucht : FlyMode("Minesucht") {
    private var minesuchtTP = 0L

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (!mc.gameSettings.keyBindForward.isKeyDown) return

            if (System.currentTimeMillis() - minesuchtTP > 99) {
                val vec = eyes + getLook(1f) * 7.0

                if (fallDistance > 0.8) {
                    sendPackets(
                        C04PacketPlayerPosition(posX, posY + 50, posZ, false),
                        C04PacketPlayerPosition(posX, posY + 20, posZ, true)
                    )
                    fall(100f, 100f)
                    fallDistance = 0f
                }
                sendPackets(
                    C04PacketPlayerPosition(vec.xCoord, posY + 50, vec.zCoord, true),
                    C04PacketPlayerPosition(posX, posY, posZ, false),
                    C04PacketPlayerPosition(vec.xCoord, posY, vec.zCoord, true),
                    C04PacketPlayerPosition(posX, posY, posZ, false)
                )
                minesuchtTP = System.currentTimeMillis()
            } else {
                sendPackets(
                    C04PacketPlayerPosition(posX, posY, posZ, false),
                    C04PacketPlayerPosition(posX, posY, posZ, true)
                )
            }
        }
    }
}
