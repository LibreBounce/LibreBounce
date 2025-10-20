/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.findItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar.resetSlot
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar.selectSlotSilently
import net.minecraft.init.Items
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement

object KeepAlive : Module("KeepAlive", Category.PLAYER) {

    val mode by choices("Mode", arrayOf("/heal", "Soup"), "/heal")

    private var runOnce = false

    val onMotion = handler<MotionEvent> {
        val player = mc.thePlayer ?: return@handler

        if (player.isDead || player.health <= 0) {
            if (runOnce) return@handler

            when (mode) {
                "/heal" -> player.sendChatMessage("/heal")
                "Soup" -> {
                    val soupInHotbar = findItem(36, 44, Items.mushroom_stew)

                    if (soupInHotbar != null) {
                        selectSlotSilently(
                            this,
                            soupInHotbar,
                            immediate = true,
                            render = false,
                            resetManually = true
                        )
                        sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
                        resetSlot(this)
                    }
                }
            }

            runOnce = true
        } else
            runOnce = false
    }
}