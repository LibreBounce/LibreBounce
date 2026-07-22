/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.misc.NoSlotSet
import net.ccbluex.liquidbounce.features.module.modules.render.SilentHotbarModule
import net.ccbluex.liquidbounce.features.module.modules.world.ChestAura
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.block.PlantBlock
import net.minecraft.init.Blocks.*
import net.minecraft.item.Item
import net.minecraft.item.BlockItem
import net.minecraft.network.packet.c2s.play.PlayerUseC2SPacket
import net.minecraft.network.packet.c2s.play.CloseInventoryMenuC2SPacket
import net.minecraft.network.packet.c2s.play.InventoryMenuClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.EnumState.OPEN_INVENTORY_ACHIEVEMENT
import net.minecraft.network.packet.s2c.play.SelectSlotS2CPacket
import net.minecraft.network.packet.s2c.play.OpenInventoryMenuS2CPacket
import net.minecraft.network.packet.s2c.play.CloseInventoryMenuS2CPacket

object InventoryUtils : MinecraftInstance, Listenable {
    // Is inventory open on server-side?
    var serverOpenInventory
        get() = _serverOpenInventory
        set(value) {
            if (value != _serverOpenInventory) {
                sendPacket(
                    if (value) ClientStatusC2SPacket(OPEN_INVENTORY_ACHIEVEMENT)
                    else CloseInventoryMenuC2SPacket(mc.player?.openContainer?.windowId ?: 0)
                )

                _serverOpenInventory = value
            }
        }

    var serverOpenContainer = false
        private set

    // Backing fields
    private var _serverOpenInventory = false

    var lerpedSlot = 0f

    var isFirstInventoryClick = true

    var timeSinceClosedInventory = 0L

    val CLICK_TIMER = MSTimer()

    val BLOCK_BLACKLIST = setOf(
        chest,
        ender_chest,
        trapped_chest,
        anvil,
        sand,
        web,
        torch,
        crafting_table,
        furnace,
        waterlily,
        dispenser,
        stone_pressure_plate,
        wooden_pressure_plate,
        noteblock,
        dropper,
        tnt,
        standing_banner,
        wall_banner,
        redstone_torch,
        ladder
    )

    fun findItemArray(startInclusive: Int, endInclusive: Int, items: Array<Item>): Int? {
        for (i in startInclusive..endInclusive)
            if (mc.player.openContainer.getSlot(i).stack?.item in items)
                return i - 36

        return null
    }

    fun findItem(start: Int, end: Int, item: Item): Int? {
        for (i in start..end)
            if (mc.player.openContainer.getSlot(i).stack?.item == item)
                return i - if (start == 36 && end == 44) 36 else 0

        return null
    }

    fun hasSpaceInHotbar(): Boolean {
        for (i in 36..44)
            mc.player.openContainer.getSlot(i).stack ?: return true

        return false
    }

    fun hasSpaceInInventory() = mc.player?.inventory?.firstEmptyStack != -1

    fun countSpaceInInventory() = mc.player.inventory.mainInventory.count { it.isEmpty() }

    fun findBlockInHotbar(): Int? {
        val player = mc.player ?: return null
        val inventory = player.openContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is BlockItem) (stack.item as BlockItem).block else return@filter false

            stack.item is BlockItem && stack.stackSize > 0 && block !in BLOCK_BLACKLIST && block !is PlantBlock
        }.minByOrNull { (inventory.getSlot(it).stack.item as BlockItem).block.isFullCube }?.minus(36)
    }

    fun findLargestBlockStackInHotbar(): Int? {
        val player = mc.player ?: return null
        val inventory = player.openContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is BlockItem) (stack.item as BlockItem).block else return@filter false

            stack.item is BlockItem && stack.stackSize > 0 && block.isFullCube && block !in BLOCK_BLACKLIST && block !is PlantBlock
        }.maxByOrNull { inventory.getSlot(it).stack.stackSize }?.minus(36)
    }

    fun findBlockStackInHotbarGreaterThan(amount: Int): Int? {
        val player = mc.player ?: return null
        val inventory = player.openContainer

        return (36..44).filter {
            val stack = inventory.getSlot(it).stack ?: return@filter false
            val block = if (stack.item is BlockItem) (stack.item as BlockItem).block else return@filter false

            stack.item is BlockItem && stack.stackSize > amount && block.isFullCube && block !in BLOCK_BLACKLIST && block !is PlantBlock
        }.minByOrNull { (inventory.getSlot(it).stack.item as BlockItem).block.isFullCube }?.minus(36)
    }

    // Converts container slot to hotbar slot id, else returns null
    fun Int.toHotbarIndex(stacksSize: Int): Int? {
        val parsed = this - stacksSize + 9

        return if (parsed in 0..8) parsed else null
    }

    fun blocksAmount(): Int {
        val player = mc.player ?: return 0
        var amount = 0

        for (i in 36..44) {
            val stack = player.inventorySlot(i).stack ?: continue
            val item = stack.item
            if (item is BlockItem) {
                val block = item.block
                val displayItemInHand = player.displayItemInHand
                if (displayItemInHand != null && displayItemInHand == stack || block !in BLOCK_BLACKLIST && block !is PlantBlock) {
                    amount += stack.stackSize
                }
            }
        }

        return amount
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.isCancelled) return@handler

        when (val packet = event.packet) {
            is PlayerUseC2SPacket, is InventoryMenuClickSlotC2SPacket -> {
                CLICK_TIMER.reset()

                if (packet is InventoryMenuClickSlotC2SPacket)
                    isFirstInventoryClick = false
            }

            is ClientStatusC2SPacket ->
                if (packet.status == OPEN_INVENTORY_ACHIEVEMENT) {
                    if (_serverOpenInventory) event.cancelEvent()
                    else {
                        isFirstInventoryClick = true
                        _serverOpenInventory = true
                    }
                }

            is CloseInventoryMenuC2SPacket, is CloseInventoryMenuS2CPacket, is OpenInventoryMenuS2CPacket -> {
                isFirstInventoryClick = false
                _serverOpenInventory = false
                serverOpenContainer = false

                timeSinceClosedInventory = System.currentTimeMillis()

                if (packet is OpenInventoryMenuS2CPacket) {
                    if (packet.guiId == "minecraft:chest" || packet.guiId == "minecraft:container")
                        serverOpenContainer = true
                } else
                    ChestAura.tileTarget = null
            }

            is SelectSlotS2CPacket -> {
                if (SilentHotbar.currentSlot == packet.displayItemInHandHotbarIndex)
                    return@handler

                SilentHotbar.ignoreSlotChange = true

                val previousSlot = SilentHotbar.currentSlot

                if (NoSlotSet.handleEvents()) {
                    WaitTickUtils.conditionalSchedule {
                        if (SilentHotbar.currentSlot == packet.displayItemInHandHotbarIndex) {
                            mc.player?.inventory?.currentItem = previousSlot

                            return@conditionalSchedule true
                        }

                        false
                    }
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val module = SilentHotbarModule

        val slotToUse = SilentHotbar.renderSlot(module.handleEvents() && module.keepHotbarSlot).toFloat()

        lerpedSlot = (lerpedSlot..slotToUse).lerpWith(RenderUtils.deltaTimeNormalized())
    }

    val onWorld = handler<WorldEvent> {
        SilentHotbar.resetSlot()

        _serverOpenInventory = false
        serverOpenContainer = false
    }


}
