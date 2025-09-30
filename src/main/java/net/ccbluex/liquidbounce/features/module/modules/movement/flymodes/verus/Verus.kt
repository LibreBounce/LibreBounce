package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.boostMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.boostTicksValue
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.damage
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.timerSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.yBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.component3
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

/**
 * Ported from the VerusDamage Script by Arcane
 *
 * Notes:
 * - Going below a block (Like the Updated NCP fly), should help to temporarily bypass Speed A Checks
 * - Turning off Damage should bypass Fly G Checks
 */
object Verus : FlyMode("Verus") {

    private var boostTicks = 0
    private var damaged = false

    override fun onEnable() {
        val player = mc.thePlayer ?: return
        val (x, y, z) = player

        boostTicks = 0
        if (mc.theWorld.getCollidingBoundingBoxes(
                player,
                player.entityBoundingBox.offset(0.0, 3.0001, 0.0).expand(0.0, 0.0, 0.0)
            ).isEmpty()
        ) {
            if (damage) {
                sendPackets(
                    C04PacketPlayerPosition(x, y + 3.0001, z, false),
                    C06PacketPlayerPosLook(x, y, z, player.rotationYaw, player.rotationPitch, false),
                    C06PacketPlayerPosLook(x, y, z, player.rotationYaw, player.rotationPitch, true)
                )
                damaged = true
            } else {
                damaged = true
            }
        }
        player.setPosition(player.posX, player.posY + yBoost.toDouble(), player.posZ)
    }

    override fun onDisable() {
        damaged = false

        if (boostTicks > 0) {
            mc.thePlayer?.stopXZ()
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        player?.stopXZ()
        player?.stop()

        if (boostTicks == 0 && player.hurtTime > 0) {
            boostTicks = boostTicksValue
        }

        boostTicks--

        if (timerSlow) {
            if (player.ticksExisted % 3 == 0) {
                mc.timer.timerSpeed = 0.15f
            } else {
                mc.timer.timerSpeed = 0.08f
            }
        }

        strafe(boostMotion, true)
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && damaged) {
            packet.onGround = true
        }
    }

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
    }
}