package net.ccbluex.liquidbounce.interfaces;

import net.ccbluex.liquidbounce.utils.item.DataPackBypass;

import javax.annotation.Nullable;

/**
 * Additions for {@link net.minecraft.item.Item} because they removed ArmorItem in 1.21.5, and I'm lazy
 */
public interface ItemAddition {
	@Nullable
	DataPackBypass liquid_bounce$getArmorItem();
}
