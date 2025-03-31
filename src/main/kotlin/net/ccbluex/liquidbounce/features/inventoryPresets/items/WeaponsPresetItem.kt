package net.ccbluex.liquidbounce.features.inventoryPresets.items

import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.item.SwordItem
import kotlin.math.ceil
import kotlin.math.pow

data object WeaponsPresetItem : PresetItem(
    type = ItemType.WEAPONS,
    comparatorChain = ComparatorChain(
        compareBy { it.estimateDamage() },
        compareBy { SECONDARY_VALUE_ESTIMATOR.estimateValue(it) },
        PREFER_BETTER_DURABILITY,
        compareBy { it.get(DataComponentTypes.ENCHANTABLE)?.value ?: 0 },
        STABILIZE_COMPARISON
    )
) {
    override fun satisfies(stack: ItemStack) = stack.item is SwordItem
}

@Suppress("MagicNumber")
private val DAMAGE_ESTIMATOR = EnchantmentValueEstimator(
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SMITE, 2.0f * 0.1f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.BANE_OF_ARTHROPODS, 2.0f * 0.1f),
    // Knockback deals no damage, but it allows us to deal more damage because we don't get hit as often.
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.2f),
)

@Suppress("MagicNumber")
private val SECONDARY_VALUE_ESTIMATOR = EnchantmentValueEstimator(
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.LOOTING, 0.05f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.05f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SWEEPING_EDGE, 0.2f),
    EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.25f),
)

@Suppress("MagicNumber")
private fun ItemStack.estimateDamage(): Double {
    val attackInterval = 20.0 / attackSpeed
    val probabilityFactor = 0.85.pow(ceil(attackInterval * 0.9) / 20.0)

    val baseDamage = attackDamage * attackSpeed * probabilityFactor
    val scaledDamage = baseDamage * (1.0 + DAMAGE_ESTIMATOR.estimateValue(this))

    val fireLevel = getEnchantment(Enchantments.FIRE_ASPECT)
    val fireDamage = (fireLevel * 4 - 1).coerceAtLeast(0) * 0.33

    return scaledDamage + fireDamage
}
