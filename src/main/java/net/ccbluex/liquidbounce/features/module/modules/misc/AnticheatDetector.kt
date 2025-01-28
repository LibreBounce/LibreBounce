/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 * @author RtxOP
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.chat

object AnticheatDetector : Module("AnticheatDetector", Category.MISC) {
    private val debug by boolean("Debug", true)

    private val actionNumbers = mutableListOf<Int>()
    private var check = false
    private var ticksPassed = 0

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is net.minecraft.network.play.server.S32PacketConfirmTransaction && check) {
            actionNumbers.add(packet.actionNumber.toInt())

            if (debug) {
                chat("ID: ${packet.actionNumber}")
            }

            if (actionNumbers.size >= 5) {
                analyzeActionNumbers()
                check = false
            }
            ticksPassed = 0
        }
    }

    val onTick = handler<GameTickEvent> {
        if (check) ticksPassed++
        if (ticksPassed > 40 && check) {
            hud.addNotification(Notification("§3Anticheat detection timed out.", 3000F))
            check = false
            actionNumbers.clear()
        }
    }

    val onWorld = handler<WorldEvent> {
        reset()
        if (it.worldClient != null) check = true
    }

    override fun onEnable() {
        reset()
        // if (mc.theWorld != null) hud.addNotification(Notification("§3Anticheat detection started...", 3000F))
    }

    private fun analyzeActionNumbers() {
        if (actionNumbers.size < 3) {
            return
        }

        val differences = mutableListOf<Int>()
        for (i in 1 until actionNumbers.size) {
            differences.add(actionNumbers[i] - actionNumbers[i - 1])
        }

        val allSame = differences.all { it == differences[0] }
        if (allSame) {
            val step = differences[0]
            val first = actionNumbers.first()

            val detectedAC = when (step) {
                1 -> when {
                    first in -23772..-23762 -> "Vulcan"  
                    first in 95..105 -> "Matrix"       
                    else -> "Verus"
                }
                -1 -> when {
                    first in -5..0 -> "Grim"       
                    first in -3005..-2995 -> "Karhu"        
                    else -> "Polar"
                }
                else -> null
            }

            detectedAC?.let {
                hud.addNotification(Notification("§3Anticheat detected: §a${it}", 3000F))
                actionNumbers.clear()
                return
            }
        }

        // Polar 
        if (differences.size >= 2) {
            val firstDiff = differences[0]
            val secondDiff = differences[1]
            val remainingDiffs = differences.drop(2)

            if (firstDiff >= 100 && secondDiff == -1 && remainingDiffs.all { it == -1 }) {
                hud.addNotification(Notification("§3Anticheat detected: §aPolar", 3000F))
                actionNumbers.clear()
                return
            }
        }

        hud.addNotification(Notification("§3No known anticheat detected.", 3000F))
        if (debug) {
            chat("§3Action Numbers: ${actionNumbers.joinToString()}")
            chat("§3Differences: ${differences.joinToString()}")
        }
        actionNumbers.clear()
    }

    private fun reset() {
        actionNumbers.clear()
        ticksPassed = 0
        check = false
    }
}