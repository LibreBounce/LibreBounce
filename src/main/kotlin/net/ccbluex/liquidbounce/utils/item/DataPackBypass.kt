package net.ccbluex.liquidbounce.utils.item

import net.minecraft.item.equipment.ArmorMaterial
import net.minecraft.item.equipment.EquipmentType

/**
 * Recreation of the armor item...
 * Made because Mojang loves screwing over mods with these garbage data packs that no one wants and no one needs,
 * will probably change this name to something more "professional" later, but I hate data packs.
 */
@JvmRecord
data class DataPackBypass(val material: ArmorMaterial, val type: EquipmentType)

