/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.aac

import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

object AAC : NoFallMode("AAC") {
    private var currentState = 0

    override fun onUpdate() {
        mc.thePlayer?.run {
            if (fallDistance > 2f) {
                sendPacket(PlayerMoveC2SPacket(true))
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