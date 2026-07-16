/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.client.options.GameOptions
import net.minecraft.network.packet.c2s.play.PlayerMovementActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMovementActionC2SPacket.Action.START_SNEAKING
import net.minecraft.network.packet.c2s.play.PlayerMovementActionC2SPacket.Action.STOP_SNEAKING

// TODO: Port all the sneak modes other than Legit to NoSlow, and a customizable speed
object Sneak : Module("Sneak", Category.MOVEMENT) {

    val mode by choices("Mode", arrayOf("Legit", "Vanilla", "Switch", "MineSecure"), "MineSecure")
    val stopMove by boolean("StopMove", false)

    private var sneaking = false

    val onMotion = handler<MotionEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (stopMove && player.isMoving) {
            if (sneaking)
                onDisable()
            return@handler
        }

        when (mode) {
            "Legit" -> mc.gameSettings.keyBindSneak.pressed = true

            "Vanilla" -> {
                if (sneaking)
                    return@handler

                sendPacket(PlayerMovementActionC2SPacket(player, START_SNEAKING))
            }

            "Switch" -> {
                when (event.eventState) {
                    EventState.PRE -> {
                        sendPackets(
                            PlayerMovementActionC2SPacket(player, START_SNEAKING),
                            PlayerMovementActionC2SPacket(player, STOP_SNEAKING)
                        )
                    }

                    EventState.POST -> {
                        sendPackets(
                            PlayerMovementActionC2SPacket(player, STOP_SNEAKING),
                            PlayerMovementActionC2SPacket(player, START_SNEAKING)
                        )
                    }

                    else -> {}
                }
            }

            "MineSecure" -> {
                if (event.eventState == EventState.PRE)
                    return@handler

                sendPacket(PlayerMovementActionC2SPacket(player, START_SNEAKING))
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        sneaking = false
    }

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        if (mode == "Legit") {
            if (!GameOptions.isKeyDown(mc.gameSettings.keyBindSneak)) {
                mc.gameSettings.keyBindSneak.pressed = false
            }
        } else {
            (PlayerMovementActionC2SPacket(player, STOP_SNEAKING))
        }

        sneaking = false
    }
}