package net.ccbluex.liquidbounce.features.module.modules.misc

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.ccbluex.liquidbounce.event.events.ItemLoreQueryEvent
import net.ccbluex.liquidbounce.event.events.PlayerEquipmentChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.module.CommandInvsee
import net.ccbluex.liquidbounce.features.command.commands.module.NoInteractInventory
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.utils.client.asText
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.EquipmentSlot.MAINHAND
import net.minecraft.entity.EquipmentSlot.OFFHAND
import net.minecraft.entity.EquipmentSlot.Type.*
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

/**
 * Module InventoryTracker
 *
 * Tracks the inventories of other players.
 *
 * Command: [CommandInvsee]
 */
object ModuleInventoryTracker : ClientModule("InventoryTracker", Category.MISC) {

    private val playerMap = HashMap<UUID, TrackedInventory>()

    @Suppress("unused")
    val playerEquipmentChangeHandler = handler<PlayerEquipmentChangeEvent> { event ->
        val player = event.player
        if (player !is OtherClientPlayerEntity || ModuleAntiBot.isBot(player)) {
            return@handler
        }

        val updatedSlot = event.equipmentSlot
        if (updatedSlot.type == ANIMAL_ARMOR) {
            return@handler
        }

        val newItemStack = event.itemStack

        val mainHandStack = if (updatedSlot == MAINHAND) newItemStack else player.mainHandStack
        val offHandStack = if (updatedSlot == OFFHAND) newItemStack else player.offHandStack

        val trackedInventory = playerMap.getOrPut(player.uuid) { TrackedInventory() }

        when (updatedSlot.type) {
            HAND -> {
                trackedInventory.update(offHandStack, OFFHAND)
                trackedInventory.update(mainHandStack, MAINHAND)
            }

            HUMANOID_ARMOR -> trackedInventory.update(newItemStack, updatedSlot)
            else -> {}
        }

        val inventory = player.inventory
        val items = trackedInventory.items.toTypedArray()

        val mainHandEmpty = mainHandStack.isEmpty
        val range = if (mainHandEmpty) 0..34 else 1..35
        val offset = if (mainHandEmpty) 1 else 0

        for (i in range) {
            inventory.main[i + offset] = items.getOrNull(i) ?: ItemStack.EMPTY
        }
    }

    override fun disable() = reset()

    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private fun reset() {
        playerMap.keys.forEach { uuid ->
            val player = world.players.find { it.uuid == uuid } ?: return@forEach
            for (i in 1 until player.inventory.main.size) {
                player.inventory.main[i] = ItemStack.EMPTY
            }
        }

        playerMap.clear()
    }

    @Suppress("unused")
    val itemLoreQueryHandler = handler<ItemLoreQueryEvent> { event ->
        if (!running || mc.currentScreen !is NoInteractInventory) {
            return@handler
        }

        val player = CommandInvsee.viewedPlayer ?: return@handler
        val timeStamp = playerMap[player.uuid]?.timeMap?.getLong(event.itemStack)?.takeIf { it != 0L } ?: return@handler
        val lastSeen = System.currentTimeMillis() - timeStamp
        event.lore.add(
            Text.empty().styled { it.withFormatting(Formatting.RESET) }.append(
                "Last Seen: ${toMinutesSeconds(lastSeen)}".asText().styled { it.withFormatting(Formatting.GRAY) }
            )
        )
    }

    private fun toMinutesSeconds(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

class TrackedInventory {

    val items = ArrayDeque<ItemStack>()
    val timeMap = Object2LongOpenHashMap<ItemStack>()

    /**
     * if slot type is armor then we check if the item is already in the tracked items
     * and if yes we remove it because it has been equipped
     */
    fun update(newItemStack: ItemStack, updatedSlot: EquipmentSlot) {
        items.removeIf { it.count == 0 }
        if (newItemStack.isEmpty) {
            return
        }

        items.removeIf { newItemStack.item == it.item && newItemStack.enchantments == it.enchantments }
        if (updatedSlot.type == HAND) {
            items.addFirst(newItemStack)
            timeMap.put(newItemStack, System.currentTimeMillis())

            if (items.size > 36) {
                items.removeLast()
            }
        }
    }

}
