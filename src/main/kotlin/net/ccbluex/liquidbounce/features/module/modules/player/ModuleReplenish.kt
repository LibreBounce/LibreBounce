package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType

/**
 * Module Replenish
 *
 * Automatically refills your hotbar with items from your inventory when the count drops to a certain threshold.
 *
 * @author ccetl
 */
object ModuleReplenish : ClientModule("Replenish", Category.PLAYER) {

    private val constraints = tree(PlayerInventoryConstraints())
    private val threshold by int("Threshold", 5, 1..63)
    private val delay by int("Delay", 40, 0..1000, "ms")
    private val usePickupAll by boolean("UsePickupAll", false)
    private val insideOfChests by boolean("InsideOfChests", false)
    private val insideOfInventories by boolean("InsideOfInventories", false)

    private val chronometer = Chronometer()

    @Suppress("unused")
    private val inventoryScheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (!chronometer.hasElapsed(delay.toLong())) {
            return@handler
        }

        chronometer.reset()

        Slots.Hotbar.slots.forEach { slot ->
            val itemStack = slot.itemStack
            val unsupportedStackSize = itemStack.item.maxCount <= threshold
            if (itemStack.isEmpty || unsupportedStackSize || itemStack.count > threshold) {
                return@forEach
            }

            val inventorySlots = Slots.Inventory.slots
                .filter { it.itemStack.item == itemStack.item }
                .sortedByDescending { it.itemStack.count }
            if (inventorySlots.isEmpty()) {
                return@forEach
            }

            if (usePickupAll) {
                event.schedule(
                    constraints,
                    ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP),
                    ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP_ALL),
                    ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP)
                )

                return@handler
            }

            refillNormal(itemStack, inventorySlots, slot, event)

            return@handler
        }
    }

    private fun refillNormal(
        itemStack: ItemStack,
        inventorySlots: List<InventoryItemSlot>,
        slot: HotbarItemSlot,
        event: ScheduleInventoryActionEvent
    ) {
        var neededToRefill = itemStack.item.maxCount - itemStack.count
        inventorySlots.forEach { inventorySlot ->
            neededToRefill -= inventorySlot.itemStack.count
            val actions = mutableListOf(
                ClickInventoryAction.click(null, inventorySlot, 0, SlotActionType.PICKUP),
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP)
            )

            if (neededToRefill < 0) {
                actions += ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP)
            }

            event.schedule(constraints, actions)

            if (neededToRefill <= 0) {
                return
            }
        }
    }

    override val running: Boolean
        get() = super.running &&
            (!insideOfChests || (mc.currentScreen !is ShulkerBoxScreen || mc.currentScreen is InventoryScreen)) &&
            (!insideOfInventories || mc.currentScreen !is InventoryScreen)

}
