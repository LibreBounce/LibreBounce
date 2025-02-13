@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.KeyEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleElytraSwap.constraints
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import org.lwjgl.glfw.GLFW

private var swapRequested = false

private val slots = Slots.Hotbar + Slots.Inventory + Slots.OffHand
private val chestplateSlot = ArmorItemSlot(2 /* chestplate */)

/**
 * ModuleElytraSwap
 *
 * Allows to quickly replace the chestplate with an elytra and vice versa
 *
 * @author sqlerrorthing
 * @since 2/13/2025
 **/
object ModuleElytraSwap : ClientModule("ElytraSwap", Category.MISC) {
    internal val constraints = tree(PlayerInventoryConstraints())
    private val swapKey by key("Swap", GLFW.GLFW_KEY_UNKNOWN)

    @Suppress("unused")
    private val keyboardHandler = handler<KeyEvent> {
        if (it.action != GLFW.GLFW_PRESS) {
            return@handler
        }

        when (it.key.code) {
            swapKey.code -> {
                swapRequested = true
            }
        }
    }

    @Suppress("unused")
    private val elytraSwapHandler = handler<ScheduleInventoryActionEvent>(priority = 500) { event ->
        if (!swapRequested) {
            return@handler
        } else {
            swapRequested = false
        }

        val elytra = slots.findSlot(Items.ELYTRA)
        val chestplate = slots.findSlot { it.isChestplate() }

        with (chestplateSlot.itemStack /* worn item */) {
            when {
                // put on elytra
                isEmpty && elytra != null -> event.doSwap(elytra)

                // replacing of elytra with a chestplate
                isElytra() && chestplate != null -> event.doSwap(chestplate)

                // replacing the chestplate with elytra
                isChestplate() && elytra != null -> event.doSwap(elytra)
            }
        }
    }
}

private inline fun ScheduleInventoryActionEvent.doSwap(slot: ItemSlot) = schedule(
    constraints,
    ClickInventoryAction.performPickup(slot = slot),
    ClickInventoryAction.performPickup(slot = chestplateSlot),
    ClickInventoryAction.performPickup(slot = slot)
)

private inline fun ItemStack.isChestplate() = with(this.item) {
    this is ArmorItem &&
        this == Items.LEATHER_CHESTPLATE
        || this == Items.CHAINMAIL_CHESTPLATE
        || this == Items.IRON_CHESTPLATE
        || this == Items.GOLDEN_CHESTPLATE
        || this == Items.NETHERITE_CHESTPLATE
        || this == Items.DIAMOND_CHESTPLATE
}

private inline fun ItemStack.isElytra() = this.item == Items.ELYTRA
