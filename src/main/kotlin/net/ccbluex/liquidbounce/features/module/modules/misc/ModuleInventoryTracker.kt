package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.client.CommandInvsee
import net.ccbluex.liquidbounce.features.command.commands.client.NoInteractInventory
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.EquipmentSlot.MAINHAND
import net.minecraft.entity.EquipmentSlot.OFFHAND
import net.minecraft.entity.EquipmentSlot.Type.*
import net.minecraft.item.ItemStack
import java.util.*

object ModuleInventoryTracker : Module("InventoryTracker", Category.MISC) {

    private val playerMap = HashMap<UUID, TrackedInventory>()

    val entityEquipmentChangeHandler = handler<EntityEquipmentChangeEvent> { event ->
        val player = event.entity
        if (player !is OtherClientPlayerEntity || ModuleAntiBot.isBot(player)) return@handler

        val updatedSlot = event.equipmentSlot
        if (updatedSlot.type == ANIMAL_ARMOR) return@handler

        val newItemStack = event.itemStack

        val mainHandStack = if (updatedSlot == MAINHAND) newItemStack else player.mainHandStack
        val offHandStack = if (updatedSlot == OFFHAND) newItemStack else player.offHandStack

        val trackedInventory = playerMap.getOrPut(player.uuid) { TrackedInventory() }

        when (updatedSlot.type) {
            HAND -> {
                trackedInventory.update(offHandStack, OFFHAND)
                trackedInventory.update(mainHandStack, MAINHAND)
            }
            HUMANOID_ARMOR -> {
                trackedInventory.update(newItemStack, updatedSlot)
            }
            else -> {}
        }

        val inventory = player.inventory
        val items = trackedInventory.items

        val mainHandEmpty = mainHandStack.isEmpty
        val range = if (mainHandEmpty) 0..34 else 1..35
        val offset = if (mainHandEmpty) 1 else 0

        for (i in range) {
            inventory.main[i + offset] = if (i < items.size) items[i] else ItemStack.EMPTY
        }
    }

    override fun disable() = reset()

    val worldChangeHandler = handler<WorldChangeEvent> { reset() }

    private fun reset() {
        playerMap.keys.forEach { uuid ->
            val player = world.players.find { it.uuid == uuid } ?: return@forEach
            for (i in 1 until player.inventory.main.size) {
                player.inventory.main[i] = ItemStack.EMPTY
            }
        }
        playerMap.clear()
    }

    val loreQueryHandler = handler<LoreQueryEvent> { event ->
        if (!enabled || mc.currentScreen !is NoInteractInventory) return@handler
        val player = CommandInvsee.viewedPlayer
        val timeStamp = playerMap[player?.uuid]?.timeMap?.get(event.itemStack) ?: return@handler
        val lastSeen = System.currentTimeMillis() - timeStamp
        event.addLore("§7Last Seen: ${toMinutesSeconds(lastSeen)}§r")
    }

    private fun toMinutesSeconds(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes, seconds)
    }
}

class TrackedInventory {

    val items = LinkedList<ItemStack>()
    val timeMap = HashMap<ItemStack, Long>()

    /**
     * if slot type is armor then we check if the item is already in the tracked items
     * and if yes we remove it because it has been equipped
     */
    fun update(newItemStack: ItemStack, updatedSlot: EquipmentSlot) {
        items.removeIf { it.count == 0 }
        if (newItemStack.isEmpty) return

        items.removeIf { newItemStack.item == it.item && newItemStack.enchantments == it.enchantments }
        if (updatedSlot.type == HAND) {
            items.add(0, newItemStack)
            timeMap[newItemStack] = System.currentTimeMillis()

            if (items.size > 36) {
                items.removeLast()
            }
        }
    }
}
