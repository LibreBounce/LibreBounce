package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorKitParameters
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.minecraft.entity.EquipmentSlot
import net.minecraft.registry.tag.ItemTags

object ArmorEvaluation {
    /**
     * We expect damage to be around diamond sword hits
     */
    private const val EXPECTED_DAMAGE: Float = 6.0F

    fun findBestArmorPieces(
        slots: List<ItemSlot> = Slots.All,
        durabilityThreshold: Int = Int.MIN_VALUE
    ): Map<EquipmentSlot, ArmorPiece?> {
        val armorPiecesGroupedByType = groupArmorByType(slots)

        // We start with assuming that the best pieces are those which have the most damage points.
        var currentBestPieces = armorPiecesGroupedByType.mapValues { (_, piecesForType) ->
            piecesForType.maxByOrNull { it.toughness }
        }

        // Run some passes in which we try to find best armor pieces based on the parameters of the last pass
        for (ignored in 0 until 2) {
            val comparator = getArmorComparatorFor(currentBestPieces, durabilityThreshold)

            currentBestPieces = armorPiecesGroupedByType.mapValues { it.value.maxWithOrNull(comparator) }
        }

        return currentBestPieces
    }

    fun findBestArmorPiecesWithComparator(
        slots: List<ItemSlot> = Slots.All,
        comparator: ArmorComparator
    ): Map<EquipmentSlot, ArmorPiece?> {
        val armorPiecesGroupedByType = groupArmorByType(slots)

        return armorPiecesGroupedByType.mapValues { it.value.maxWithOrNull(comparator) }
    }

    private fun groupArmorByType(slots: List<ItemSlot>): Map<EquipmentSlot, List<ArmorPiece>> {
        val armorPiecesGroupedByType = slots.mapNotNull { slot ->
            if (slot.itemStack.isIn(ItemTags.TRIMMABLE_ARMOR)) {
                return@mapNotNull ArmorPiece(slot)
            }
        }.groupBy { t -> (t as ArmorPiece).slotType }

        // TODO: please work lol
        return armorPiecesGroupedByType as Map<EquipmentSlot, List<ArmorPiece>>
    }

    fun getArmorComparatorFor(
        currentKit: Map<EquipmentSlot, ArmorPiece?>,
        durabilityThreshold: Int = Int.MIN_VALUE
    ): ArmorComparator {
        return getArmorComparatorForParameters(
            ArmorKitParameters.getParametersForSlots(currentKit),
            durabilityThreshold
        )
    }

    fun getArmorComparatorForParameters(
        currentParameters: ArmorKitParameters,
        durabilityThreshold: Int = Int.MIN_VALUE
    ): ArmorComparator {
        return ArmorComparator(EXPECTED_DAMAGE, currentParameters, durabilityThreshold)
    }


}
