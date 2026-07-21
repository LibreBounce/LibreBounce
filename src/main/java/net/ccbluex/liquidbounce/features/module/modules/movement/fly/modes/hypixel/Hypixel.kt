/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.hypixel

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.hypixelBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.hypixelBoostDelay
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.hypixelBoostTimer
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.minecraft.init.Blocks.air
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.Box

object Hypixel : FlyMode("Hypixel") {
    private val tickTimer = TickDelayTimer(2)
    private val msTimer = MSTimer()

    override fun onEnable() {
        msTimer.reset()
        tickTimer.reset()
    }

    override fun onUpdate() {
        mc.player?.run {
            mc.timer.timerSpeed =
                if (hypixelBoost && !msTimer.hasTimePassed(hypixelBoostDelay))
                    1f + hypixelBoostTimer * (msTimer.hasTimeLeft(hypixelBoostDelay) / hypixelBoostDelay.toFloat())
                else 1f

            if (tickTimer.resetIfPassed())
                setPosition(posX, posY + 1.0E-5, posZ)
        }
    }

    override fun onPacket(event: PacketEvent) {
        if (event.packet is PlayerMoveC2SPacket)
            event.packet.onGround = false
    }

    override fun onBB(event: BlockBBEvent) {
        if (event.block == air && event.y < mc.player.posY)
            event.boundingBox = Box.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x + 1.0,
                mc.player.posY,
                event.z + 1.0
            )
    }

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
    }

    override fun onStep(event: StepEvent) {
        event.stepHeight = 0f
    }
}
