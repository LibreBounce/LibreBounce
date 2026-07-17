/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

/*
* Working on Vulcan: 2.8.8
* Tested on: eu.loyisa.cn, anticheat-test.com
* Credit: @ion1x & @Razzy52 / VulcanTP
*/
object VulcanFast288 : NoFallMode("VulcanFast2.8.8") {
    override fun onPacket(event: PacketEvent) {
        mc.player?.run {
            if (event.packet is Position) {
                val fallingPlayer = FallingPlayer()
                if (fallDistance > 2.5 && fallDistance < 50) {
                    // Checks to prevent fast falling to void.
                    if (fallingPlayer.findCollision(500) != null) {
                        event.packet.onGround = true

                        stopXZ()
                        motionY = -99.887575
                        isSneaking = true
                    }
                }
            }
        }
    }
}
