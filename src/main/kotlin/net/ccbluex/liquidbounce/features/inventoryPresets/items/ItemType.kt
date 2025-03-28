package net.ccbluex.liquidbounce.features.inventoryPresets.items

enum class ItemType {
    NONE,
    CHOOSE,
    BLOCK,
    SWORD,
    AXE,
    PICKAXE,
    FOOD,
    POTION;

    companion object {
        @JvmStatic
        fun findOrThrow(name: String) = entries.find { it.name == name } ?: error("Unknown item type $name")
    }
}
