/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.updatePlayerItem

object FakeItemRender {

    var shouldNotOverride = false

    var fakeItem = -1
        private set

    private var formerSlot = -1

    fun renderFakeItem(slot: Int) {
        if (fakeItem == -1) fakeItem = slot
    }

    fun resetFakeItem() {
        if (fakeItem != -1) fakeItem = -1
    }

    fun saveFormerSlot(slot: Int) {
        if (formerSlot == -1) formerSlot = slot
    }

    fun renderFormerSlot() {
        if (formerSlot == -1) return

        updatePlayerItem(formerSlot)
        formerSlot = -1
        shouldNotOverride = false
    }
}