package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.minecraft.item.Items

@Suppress("MagicNumber")
internal class Using : Configurable("Using") {
    private val onItemUsing by boolean("IgnoreUsingItem", false)
    private val push by int("Push", 2, 1..20, suffix = "ticks")
    private val pullback by int("Pullback", 10, 1..20, suffix = "ticks")

    internal suspend inline fun useRod(sequence: Sequence, slot: HotbarItemSlot) {
        sequence.waitTicks(push)
        SilentHotbar.selectSlotSilently(this, slot.hotbarSlotForServer, pullback)
        useHotbarSlotOrOffhand(slot, pullback)
    }

    @get:JvmSynthetic
    internal inline val canUseRod
        get() = onItemUsing
                || (player.activeItem?.item != Items.FISHING_ROD
                    && (!player.isUsingItem || !KillAuraAutoBlock.blockVisual))
}
