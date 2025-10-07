/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C03PacketPlayer

object AAC : NoFallMode("AAC") {
    private var currentState = 0

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (fallDistance > 2f) {
                sendPacket(C03PacketPlayer(true))
                currentState = 2
            } else if (currentState == 2 && fallDistance < 2) {
                motionY = 0.1
                currentState = 3
                return
            }

            when (currentState) {
                3 -> {
                    motionY = 0.1
                    currentState = 4
                }

                4 -> {
                    motionY = 0.1
                    currentState = 5
                }

                5 -> {
                    motionY = 0.1
                    currentState = 1
                }
            }
        }
    }
}