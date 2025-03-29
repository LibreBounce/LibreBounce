package net.ccbluex.liquidbounce.utils.entity

import net.minecraft.entity.EntityEquipment
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ItemStack

fun EntityEquipment.getArmor(): List<ItemStack> {
    val stacks = arrayListOf<ItemStack>()
    val armorSlots = arrayOf(
        EquipmentSlot.FEET,
        EquipmentSlot.LEGS,
        EquipmentSlot.CHEST,
        EquipmentSlot.HEAD,
        EquipmentSlot.BODY,
        EquipmentSlot.SADDLE
    )
    for (slot in armorSlots) {
        val stack = this.get(slot)
        stacks.add(stack)
    }
    return stacks.toList()
}
