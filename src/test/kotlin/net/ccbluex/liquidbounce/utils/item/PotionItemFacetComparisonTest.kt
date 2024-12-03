package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.VirtualItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.PotionItemFacet
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import org.junit.jupiter.api.Test

class PotionItemFacetComparisonTest {

    private fun ItemStack.asPotionItemFacet() = PotionItemFacet(VirtualItemSlot(this, ItemSlotType.CONTAINER, 0))

    @Test
    fun testPreferHigherTierPotions() {
        val potionS = createSplashPotion("S",
            StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 1),
        ).asPotionItemFacet()
        val potionA = createSplashPotion("A",
            StatusEffectInstance(StatusEffects.REGENERATION, 1, 1),
        ).asPotionItemFacet()
        val potionAA = createSplashPotion("AA",
            StatusEffectInstance(StatusEffects.REGENERATION, 1, 1),
            StatusEffectInstance(StatusEffects.RESISTANCE, 1, 1),
        ).asPotionItemFacet()
        val potionAB = createSplashPotion("AB",
            StatusEffectInstance(StatusEffects.REGENERATION, 1, 1),
            StatusEffectInstance(StatusEffects.STRENGTH, 1, 1),
        ).asPotionItemFacet()
        val potionAAF = createSplashPotion("AAF",
            StatusEffectInstance(StatusEffects.REGENERATION, 1, 1),
            StatusEffectInstance(StatusEffects.RESISTANCE, 1, 1),
        ).asPotionItemFacet()

        assert(potionS > potionA)

        assert(potionAA > potionAB)

        assert(potionAAF > potionAA)
    }

    @Test
    fun testPreferAmplifier() {
        val potionA2A1 = createSplashPotion("A2A1",
            StatusEffectInstance(StatusEffects.ABSORPTION, 1, 2),
            StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1, 1),
        ).asPotionItemFacet()
        val potionA1A1 = createSplashPotion("A1A1",
            StatusEffectInstance(StatusEffects.RESISTANCE, 1, 1),
            StatusEffectInstance(StatusEffects.HEALTH_BOOST, 1, 1),
        ).asPotionItemFacet()

        assert(potionA2A1 > potionA1A1)
    }

    @Test
    fun testPreferHigherDurationPotions() {
        val potionS60 = createSplashPotion("S60",
            StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 60, 1),
        ).asPotionItemFacet()
        val potionS30 = createSplashPotion("S30",
            StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 30, 1),
        ).asPotionItemFacet()
        val potionS20A1200 = createSplashPotion("S20A1200",
            StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 20, 1),
            StatusEffectInstance(StatusEffects.REGENERATION, 1200, 1),
        ).asPotionItemFacet()
        val potionS30A60 = createSplashPotion("S30A60",
            StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 30, 1),
            StatusEffectInstance(StatusEffects.REGENERATION, 60, 1),
        ).asPotionItemFacet()

        assert(potionS60 > potionS30)

        assert(potionS20A1200 < potionS30A60)
    }

}
