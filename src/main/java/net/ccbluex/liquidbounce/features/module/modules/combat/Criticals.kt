/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.Fly
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.living.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Position

object Criticals : Module("Criticals", Category.COMBAT) {

    val mode by choices(
        "Mode",
        arrayOf(
            "Packet",
            "NCPPacket",
            "BlocksMC",
            "BlocksMC2",
            "NoGround",
            "Hop",
            "TPHop",
            "Jump",
            "LowJump",
            "CustomMotion",
            "Visual"
        ),
        "Packet"
    )

    val delay by int("Delay", 0, 0..500, suffix = "ms")
    private val damagedTimer by int("HurtTime", 10, 0..10)
    private val customMotionY by float("CustomY", 0.2f, 0.01f..0.42f) { mode == "CustomMotion" }

    val msTimer = MSTimer()

    override fun onEnable() {
        if (mode == "NoGround")
            mc.player.tryJump()
    }

    val onAttack = handler<AttackEvent> { event ->
        if (event.targetEntity is LivingEntity) {
            val player = mc.player ?: return@handler
            val entity = event.targetEntity

            if (!player.onGround || player.isOnLadder || player.inCobweb || player.isInLiquid ||
                player.ridingEntity != null || entity.damagedTimer > damagedTimer ||
                Fly.handleEvents() || !msTimer.hasTimePassed(delay)
            )
                return@handler

            val (x, y, z) = player

            when (mode) {
                "Packet" -> {
                    sendPackets(
                        Position(x, y + 0.0625, z, true),
                        Position(x, y, z, false)
                    )
                }

                "NCPPacket" -> {
                    sendPackets(
                        Position(x, y + 0.11, z, false),
                        Position(x, y + 0.1100013579, z, false),
                        Position(x, y + 0.0000013579, z, false)
                    )
                }

                "BlocksMC" -> {
                    sendPackets(
                        Position(x, y + 0.001091981, z, true),
                        Position(x, y, z, false)
                    )
                }

                "BlocksMC2" -> {
                    if (player.ticksExisted % 4 == 0) {
                        sendPackets(
                            Position(x, y + 0.0011, z, true),
                            Position(x, y, z, false)
                        )
                    }
                }

                "Hop" -> {
                    player.motionY = 0.1
                    player.fallDistance = 0.1f
                    player.onGround = false
                }

                "TPHop" -> {
                    sendPackets(
                        Position(x, y + 0.02, z, false),
                        Position(x, y + 0.01, z, false)
                    )
                    player.setPosition(x, y + 0.01, z)
                }

                "Jump" -> player.motionY = 0.42
                "LowJump" -> player.motionY = 0.3425
                "CustomMotion" -> player.motionY = customMotionY.toDouble()
                "Visual" -> player.addCritParticles(entity)
            }

            msTimer.reset()
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is PlayerMoveC2SPacket && mode == "NoGround")
            event.packet.onGround = false
    }

    override val tag
        get() = mode
}
