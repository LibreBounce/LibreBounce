package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.item.isSword
import net.minecraft.item.AxeItem
import net.minecraft.item.Item

@Suppress("unused")
enum class KillAuraRequirements(
    override val choiceName: String,
    val meets: () -> Boolean
) : NamedChoice {
    CLICK("Click", {
        mc.options.attackKey.isPressedOnAny
    }),
    WEAPON("Weapon", {
        player.mainHandStack.item.isWeapon()
    });
}

/**
 * Check if the item is a weapon.
 */
private fun Item.isWeapon() = !isOlderThanOrEqual1_8 && this is AxeItem || this.isSword
