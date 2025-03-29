package net.ccbluex.liquidbounce.features.inventoryPresets.items

enum class ItemType {
    ANY,
    CHOOSE,
    BLOCKS,
    WEAPONS,
    TOOLS,
    FOOD;

    companion object {
        @JvmStatic
        fun findOrThrow(name: String) = entries.find { it.name == name } ?: error("Unknown item type $name")
    }
}
