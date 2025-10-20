/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

// TODO: Maybe this module is best suited in the Miscellaneous category?
object AutoPlay : Module("AutoPlay", Category.PLAYER, gameDetecting = false) {

    // TODO: Custom mode, allowing for any command to be executed after a game ends
    private val mode by choices("Mode", arrayOf("Paper", "Hypixel"), "Paper")

    // Hypixel Settings
    private val hypixelMode by choices("HypixelMode", arrayOf("Requeue", "Skywars", "Bedwars"), "Requeue") {
        mode == "Hypixel"
    }
    private val skywarsMode by choices("SkywarsMode", arrayOf("SoloNormal", "SoloInsane"), "SoloNormal") {
        mode == "Hypixel" && hypixelMode == "Skywars"
    }
    private val bedwarsMode by choices("BedwarsMode", arrayOf("Solo", "Double", "Trio", "Quad"), "Solo") {
        mode == "Hypixel" && hypixelMode == "Bedwars"
    }

    private val delay by int("Delay", 50, 0..200, suffix = "ticks")

    private val notification by boolean("Notification", false).subjective()

    private var delayTick = 0

    val onGameTick = handler<GameTickEvent> {
        val player = mc.thePlayer ?: return@handler

        if (!playerInGame() || !player.inventory.hasItemStack(ItemStack(Items.paper))) {
            if (delayTick > 0)
                delayTick = 0

            return@handler
        } else {
            delayTick++
        }

        if (notification && delayTick >= delay) hud.addNotification(Notification.informative(this, "Sending you to a new game!", 2000L))

        when (mode) {
            "Paper" -> {
                val paper = InventoryUtils.findItem(36, 44, Items.paper) ?: return@handler

                SilentHotbar.selectSlotSilently(this, paper, immediate = true, resetManually = true)

                if (delayTick >= delay) {
                    mc.playerController.sendUseItem(player, mc.theWorld, player.hotBarSlot(paper).stack)
                    delayTick = 0
                }
            }

            "Hypixel" -> {
                if (delayTick >= delay) {
                    when (hypixelMode) {
                        "Skywars" -> when (skywarsMode) {
                            "SoloNormal" -> player.sendChatMessage("/play solo_normal")
                            "SoloInsane" -> player.sendChatMessage("/play solo_insane")
                        }

                        "Bedwars" -> when (bedwarsMode) {
                            "Solo" -> player.sendChatMessage("/play bedwars_eight_one")
                            "Double" -> player.sendChatMessage("/play bedwars_eight_two")
                            "Trio" -> player.sendChatMessage("/play bedwars_four_three")
                            "Quad" -> player.sendChatMessage("/play bedwars_four_four")
                        }

                        else -> player.sendChatMessage("/requeue")
                    }

                    delayTick = 0
                }
            }
        }
    }

    /**
     * Check whether player is in game or not
     */
    // TODO: Maybe this should be a job for GameDetector?
    private fun playerInGame(): Boolean {
        // Isn't the null check already covered on the game tick event above?
        val player = mc.thePlayer ?: return false

        return player.ticksExisted >= 20
                && (player.capabilities.isFlying
                || player.capabilities.allowFlying
                || player.capabilities.disableDamage)
    }

    override val tag
        get() = mode
}
