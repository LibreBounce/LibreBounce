package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEqual1_19_4
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.type
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ArmorItem

object AutoArmorSaveArmor : ToggleableConfigurable(ModuleAutoArmor, "SaveArmor", true) {
    val durabilityThreshold by int("DurabilityThreshold", 24, 0..100)
    private val autoOpen by boolean("AutoOpenInventory", true)

    private var hasOpenedInventory = false

    /**
     * Opens the inventory to save armor (as if the player has opened it manually) if the following conditions are met:
     * - The module is told to save armor and there is a replacement :)
     * - The inventory constraints require open inventory
     * (Otherwise, the inventory will be open automatically in a silent way and the armor will be saved)
     * - There is no replacement from the hotbar
     * (If there are some pieces that can be replaced by the pieces from the hotbar,
     * they will be used first, without opening the inventory)
     */
    @Suppress("unused")
    private val armorAutoSaveHandler = tickHandler {
        if (!ModuleAutoArmor.running || !AutoArmorSaveArmor.enabled) {
            return@tickHandler
        }

        // the module will save armor automatically if open inventory isn't required
        if (!ModuleAutoArmor.inventoryConstraints.requiresOpenInventory || !autoOpen) {
            return@tickHandler
        }

        val armorToEquipWithSlots = ArmorEvaluation
            .findBestArmorPieces(durabilityThreshold = durabilityThreshold)
            .values
            .filterNotNull()
            .filter { !it.isAlreadyEquipped && it.itemSlot.itemStack.item is ArmorItem }

        val hasAnyHotBarReplacement = ModuleAutoArmor.useHotbar && isNewerThanOrEqual1_19_4 &&
            armorToEquipWithSlots.any { it.itemSlot is HotbarItemSlot }

        // the new pieces from the hotbar have a higher priority
        // due to the replacement speed (it's much faster, it makes sense to replace them first),
        // so it waits until all pieces from hotbar are replaced
        if (hasAnyHotBarReplacement) {
            return@tickHandler
        }

        val playerArmor = player.inventory.armor.filter { it.item is ArmorItem }
        val armorToEquip = armorToEquipWithSlots.map { it.itemSlot.itemStack.item as ArmorItem }

        val hasArmorToReplace = playerArmor.any { armorStack ->
            armorStack.durability <= durabilityThreshold &&
                armorToEquip.any { it.type() == (armorStack.item as ArmorItem).type() }
        }

        // closes the inventory if the armor is replaced.
        closeInventory(hasArmorToEquip = armorToEquip.isNotEmpty())

        // tries to close the previous screen and open the inventory
        openInventory(hasArmorToReplace = hasArmorToReplace)
    }

    /**
     * Waits and closes the inventory after the armor is replaced.
     */
    private suspend fun Sequence.closeInventory(hasArmorToEquip: Boolean) {
        if (!hasOpenedInventory || hasArmorToEquip) {
            return
        }

        this@AutoArmorSaveArmor.hasOpenedInventory = false
        waitTicks(ModuleAutoArmor.inventoryConstraints.closeDelay.random())

        // the current screen might change while the module is waiting
        if (mc.currentScreen is InventoryScreen) {
            player.closeHandledScreen()
        }
    }

    /**
     * Closes the previous game screen and opens the inventory.
     */
    private suspend fun Sequence.openInventory(hasArmorToReplace : Boolean) {
        while (hasArmorToReplace && mc.currentScreen !is InventoryScreen) {

            if (mc.currentScreen is HandledScreen<*>) {
                // closes chests/crating tables/etc.
                // TODO: well, it doesn't... :(
                //  When the player is in a chest/anvil/crafting table/etc.,
                //  hasArmorToReplace is always false...
                //  The server simply doesn't let the player know anything new about his armor :/
                //  the client knows only the state of the armor before opening the screen,
                //  the client doesn't receive any updates on the armor slots until the screen is closed.
                //  However, the client still gets updates on the armor of other players :/

                // TODO: since the client get no updates on the armor while a chest/crating table/etc. is open,
                //  try to approximately track the durability of the player's armor manually
                //  when the player receives damage and chest/crating table/etc. is open :)
                player.closeHandledScreen()
            } else if (mc.currentScreen != null) {
                // closes ClickGUI, game chat, etc. to save some armor :)
                mc.currentScreen!!.close()
            }

            waitTicks(1)    // TODO: custom delay?

            // again, the current screen might change while the module is waiting
            if (mc.currentScreen == null) {
                mc.setScreen(InventoryScreen(player))
                hasOpenedInventory = true
            }
        }
    }
}
