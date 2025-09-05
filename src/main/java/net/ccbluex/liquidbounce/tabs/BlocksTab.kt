/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.tabs

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.init.Blocks.*
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class BlocksTab : CreativeTabs("Special blocks") {

    private val itemStacks by lazy(LazyThreadSafetyMode.NONE) {
        arrayOf(
            ItemStack(command_block),
            ItemStack(Items.command_block_minecart),
            ItemStack(barrier),
            ItemStack(dragon_egg),
            ItemStack(brown_mushroom_block),
            ItemStack(red_mushroom_block),
            ItemStack(farmland),
            ItemStack(mob_spawner),
            ItemStack(lit_furnace)
        )
    }

    /**
     * Initialize of special blocks tab
     */
    init {
        backgroundImageName = "item_search.png"
    }

    /**
     * Add all items to tab
     *
     * @param itemList list of tab items
     */
    override fun displayAllReleventItems(itemList: MutableList<ItemStack>) {
        itemList += itemStacks
    }

    /**
     * Return icon item of tab
     *
     * @return icon item
     */
    override fun getTabIconItem(): Item = ItemStack(Blocks.command_block).item

    /**
     * Return name of tab
     *
     * @return tab name
     */
    override fun getTranslatedTabLabel() = "Special blocks"

    /**
     * @return searchbar status
     */
    override fun hasSearchBar() = true
}