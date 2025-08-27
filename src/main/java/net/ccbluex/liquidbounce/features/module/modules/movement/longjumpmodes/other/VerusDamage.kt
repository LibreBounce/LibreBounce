/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump.autoDisable
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

object VerusDamage : LongJumpMode("VerusDamage") {

    var damaged = false

    override fun onEnable() {
        val player = mc.thePlayer ?: return
        val (x, y, z) = player

        // Otherwise you'll get flagged
        if (!player.isMoving) {
            chat("§8[§c§lVerusDamage-§a§lFly§8] §cPlease move while toggling LongJump. Using AutoJump option is recommended.")
            return
        }

        // Note: you'll flag once for Fly G (tested on the CCBlueX Test Server)
        C04PacketPlayerPosition(x, y + 3.0001, z, false),
        C06PacketPlayerPosLook(x, y, z, player.rotationYaw, player.rotationPitch, false),
        C06PacketPlayerPosLook(x, y, z, player.rotationYaw, player.rotationPitch, true)
        damaged = true
    }

    override fun onDisable() {
        damaged = false
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInLiquid || player.isInWeb || player.isOnLadder) {
            LongJump.state = false
            return
        }

        /**
         * You can long jump up to 13-14+ blocks
         */
        if (damaged && player.isMoving) {
            player.jumpMovementFactor = 0.15f
            player.motionY += 0.015f

            // player onGround checks will not work due to sendPacket ground, therefore motionY is used instead
            if (autoDisable && player.motionY <= -0.4330104027478734) {
                player.stopXZ()
                LongJump.state = false
            }
        } else if (autoDisable) {
            LongJump.state = false
        }
    }
}