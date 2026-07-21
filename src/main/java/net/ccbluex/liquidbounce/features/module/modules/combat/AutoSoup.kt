/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.sendUseItem
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.isFirstInventoryClick
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.minecraft.client.gui.screen.inventory.menu.SurvivalInventoryScreen
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action.DROP_ITEM
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

object AutoSoup : Module("AutoSoup", Category.COMBAT) {

    // TODO: Separate hotbar & inventory delays
    private val health by float("Health", 15f, 0f..20f)
    private val delay by intRange("Delay", 150..150, 0..500, suffix = "ms")

    private val openInventory by boolean("OpenInv", true)
    private val startDelay by intRange("StartDelay", 100..100, 0..1000, suffix = "ms") { openInventory }
    private val autoClose by boolean("AutoClose", false) { openInventory }
    private val autoCloseNoSoup by boolean("AutoCloseNoSoup", true) { autoClose }
    private val autoCloseDelay by intRange("CloseDelay", 500..500, 0..1000, suffix = "ms") { openInventory && autoClose }

    private val simulateInventory by boolean("SimulateInventory", false) { !openInventory }

    private val bowl by choices("Bowl", arrayOf("Drop", "Move", "Stay"), "Drop")

    private val timer = MSTimer()
    private val startTimer = MSTimer()
    private val closeTimer = MSTimer()

    private var randomizedDelay = delay.random()
    private var randomizedStartDelay = startDelay.random()
    private var randomizedCloseDelay = autoCloseDelay.random()

    private var canCloseInventory = false

    override val tag
        get() = health.toString()

    val onGameTick = handler<GameTickEvent>(priority = -1) {
        val player = mc.player ?: return@handler

        if (!timer.hasTimePassed(randomizedDelay))
            return@handler

        val soupInHotbar = InventoryUtils.findItem(36, 44, Items.mushroom_stew)

        randomizedDelay = delay.random()

        if (player.health <= health && soupInHotbar != null) {
            SilentHotbar.selectSlotSilently(this, soupInHotbar, 1, true)

            player.sendUseItem(player.inventory.mainInventory[SilentHotbar.currentSlot])

            // Schedule slot switch the next tick as we violate vanilla logic if we do it now.
            nextTick {
                if (bowl == "Drop") {
                    if (!SilentHotbar.isSlotModified(this)) {
                        SilentHotbar.selectSlotSilently(this, soupInHotbar, 0, true)
                    }

                    sendPacket(PlayerHandActionC2SPacket(DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN))
                }

                SilentHotbar.resetSlot(this)
            }

            timer.reset()
            return@handler
        }

        val bowlInHotbar = InventoryUtils.findItem(36, 44, Items.bowl)

        if (bowl == "Move" && bowlInHotbar != null) {
            if (openInventory && mc.currentScreen !is SurvivalInventoryScreen)
                return@handler

            var bowlMovable = false

            for (i in 9..36) {
                val itemStack = player.inventory.getStackInSlot(i)

                if (itemStack == null || (itemStack.item == Items.bowl && itemStack.stackSize < 64)) {
                    bowlMovable = true
                    break
                }
            }

            if (bowlMovable) {
                if (simulateInventory)
                    serverOpenInventory = true

                mc.playerController.windowClick(0, bowlInHotbar, 0, 1, player)
            }
        }

        val soupInInventory = InventoryUtils.findItem(9, 35, Items.mushroom_stew)

        if (soupInInventory != null && InventoryUtils.hasSpaceInHotbar()) {
            if (isFirstInventoryClick && !startTimer.hasTimePassed(randomizedStartDelay)) {
                // SurvivalInventoryScreen checks have to be put separately due to problems with resetting timer.
                if (mc.currentScreen is SurvivalInventoryScreen)
                    return@handler
            } else {
                // SurvivalInventoryScreen checks have to be put separately due to problems with resetting timer.
                if (mc.currentScreen is SurvivalInventoryScreen)
                    isFirstInventoryClick = false

                startTimer.reset()
                randomizedStartDelay = startDelay.random()
            }

            if (openInventory && mc.currentScreen !is SurvivalInventoryScreen)
                return@handler

            canCloseInventory = false

            if (simulateInventory)
                serverOpenInventory = true

            mc.playerController.windowClick(0, soupInInventory, 0, 1, player)

            if (simulateInventory && mc.currentScreen !is SurvivalInventoryScreen)
                serverOpenInventory = false

            timer.reset()
            closeTimer.reset()
        } else {
            canCloseInventory = true
        }

        if (autoClose && canCloseInventory && closeTimer.hasTimePassed(randomizedCloseDelay)) {
            if (!autoCloseNoSoup && soupInInventory == null) return@handler

            if (mc.currentScreen is SurvivalInventoryScreen) {
                player?.closeScreen()
            }

            closeTimer.reset()
            randomizedCloseDelay = autoCloseDelay.random()
            canCloseInventory = false
        }
    }
}