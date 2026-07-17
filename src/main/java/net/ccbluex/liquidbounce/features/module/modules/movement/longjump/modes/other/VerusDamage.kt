/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.LongJump.autoDisable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.LongJumpMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndAngles

object VerusDamage : LongJumpMode("VerusDamage") {
    var damaged = false

    override fun onEnable() {
        mc.player?.run {
            // Otherwise you'll get flagged
            if (!isMoving) {
                chat("§8[§c§lVerusDamage-§a§lLongJump§8] §cPlease move while toggling LongJump. Using AutoJump option is recommended.")
                return
            }

            // Note: you'll flag once for Fly G (tested on the CCBlueX Test Server)
            sendPackets(
                Position(posX, posY + 3.0001, posZ, false),
                PositionAndAngles(posX, posY, posX, rotationYaw, rotationPitch, false),
                PositionAndAngles(posX, posY, posZ, rotationYaw, rotationPitch, true)
            )

            damaged = true
        }
    }

    override fun onDisable() {
        damaged = false
    }

    override fun onUpdate() {
        mc.player?.run {
            if (isInLiquid || isInWeb || isOnLadder) {
                LongJump.state = false
                return
            }

            /**
             * You can long jump up to 13-14+ blocks
             */
            if (damaged && isMoving) {
                jumpMovementFactor = 0.15f
                motionY += 0.015f

                // player onGround checks will not work due to sendPacket ground, therefore motionY is used instead
                if (autoDisable && motionY <= -0.4330104027478734) {
                    stopXZ()
                    LongJump.state = false
                }
            } else if (autoDisable) {
                LongJump.state = false
            }
        }
    }
}