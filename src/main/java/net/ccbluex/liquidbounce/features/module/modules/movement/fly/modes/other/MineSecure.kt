/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly.vanillaSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.FlyMode
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position
import kotlin.math.cos
import kotlin.math.sin

object MineSecure : FlyMode("MineSecure") {
    private val timer = MSTimer()

    override fun onUpdate() {
        mc.player?.run {
            capabilities.isFlying = false

            motionY = if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0
                else -0.01

            strafe(vanillaSpeed, true)

            if (!timer.hasTimePassed(150) || !mc.gameSettings.keyBindJump.isKeyDown)
                return

            sendPackets(
                Position(posX, posY + 5, posZ, false),
                Position(0.5, -1000.0, 0.5, false)
            )

            val yaw = rotationYaw.toRadiansD()

            setPosition(
                posX - sin(yaw) * 0.4,
                posY,
                posZ + cos(yaw) * 0.4
            )

            timer.reset()
        }
    }
}