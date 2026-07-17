/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.potion.Potion

object VulcanGround288 : SpeedMode("VulcanGround2.8.8") {
    override fun onUpdate() {
        mc.player?.run {
            if (isInLiquid || isInWeb || isOnLadder) return

            if (isMoving && collidesBottom()) {
                val speedEffect = getActivePotionEffect(Potion.moveSpeed)
                val isAffectedBySpeed = speedEffect != null && speedEffect.amplifier > 0
                val isMovingSideways = moveStrafing != 0f

                val strafe = when {
                    isAffectedBySpeed -> 0.59f
                    isMovingSideways -> 0.41f
                    else -> 0.42f
                }

                strafe(strafe)
                motionY = 0.005
            }
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket && collidesBottom()) {
            event.packet.y += 0.005
        }
    }

    private fun collidesBottom(): Boolean {
        val player = mc.player ?: return false
        mc.world ?: return false

        return mc.world.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, -0.005, 0.0)).isNotEmpty()
    }

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
    }
}
