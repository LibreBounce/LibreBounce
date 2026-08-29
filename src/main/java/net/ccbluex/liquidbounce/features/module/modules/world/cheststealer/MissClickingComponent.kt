package net.ccbluex.liquidbounce.features.module.modules.world.cheststealer

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer.lastClickIsMissClick
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer.squaredDistanceOfSlots
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.chestStealerCurrentSlot
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.clickNextTick
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.withinChance
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.Slot

open class MissClickingComponent(owner: Module): Configurable(owner.name) {
    // TODO: Add an option to not miss-click consecutively
    private val missClick by boolean("MissClick", false)
    private val missClickChance by int("MissClickChance", 4, 0..100, suffix = "%") { missClick }
    private val missClickDelay by int("MissClickDelay", 10, 0..100, suffix = "ds") { missClick }
    private val missClickChanceDistMult by boolean("MissClickChanceDistanceMultiply", true) { missClick }
    private val pauseAfterMissClick by intRange("PauseAfterMissClick", 350..650, 0..1000, suffix = "ms") { missClick }

    init {
        owner.addValues(this.values)
    }

    private var delay = missClickDelay
    private var pauseTime = pauseAfterMissClick.random().toLong()
    private var lastMiss = MSTimer()
    private var chance = missClickChance

    // Returns the delay to use
    fun tryMissClick(screen: GuiChest, targetSlot: Slot, distance: Int): Long {
        if (missClick && lastMiss.hasTimePassed(delay) && withinChance(chance)) {
            val closestEmptySlot = screen.inventorySlots.inventorySlots
                .filter { it.stack == null || it.stack.stackSize == 0 }
                .minByOrNull { otherSlot ->
                    squaredDistanceOfSlots(targetSlot.slotNumber, otherSlot.slotNumber)
                } ?: return 0

            val slotId: Int = closestEmptySlot.slotNumber ?: return 0
            pauseTime = pauseAfterMissClick.random().toLong()
            chance = missClickChance * if (missClickChanceDistMult) distance else 1
            lastMiss.reset()

            clickNextTick(slotId, 0, 1)

            /*if (itemStolenDebug)
                debug("Miss-clicked on slot $slotId. Delay until next click: ${pauseTime}ms")*/

            chestStealerCurrentSlot = slotId

            lastClickIsMissClick = true

            return pauseTime
        }

        return 0
    }
}