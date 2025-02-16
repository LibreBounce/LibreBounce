package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable

object AutoArmorSaveArmor : ToggleableConfigurable(ModuleAutoArmor, "SaveArmor", true) {
    val durabilityThreshold by int("DurabilityThreshold", 24, 0..100)
    val autoOpen by boolean("AutoOpenInventory", true)
}
