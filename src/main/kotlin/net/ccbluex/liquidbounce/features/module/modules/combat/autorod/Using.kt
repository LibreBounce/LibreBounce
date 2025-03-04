package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.network
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket

@Suppress("MagicNumber")
internal class Using : Configurable("Using") {
    private val onItemUsing by boolean("IgnoreUsingItem", false)
    private val push by int("Push", 2, 1..20, suffix = "ticks")
    private val pullback by int("Pullback", 10, 1..20, suffix = "ticks")

    internal var isRodUsing = false
    private var resetSlot: Int? = null

    internal suspend inline fun useRod(sequence: Sequence, slot: HotbarItemSlot) {
        isRodUsing = true

        sequence.waitTicks(push)
        resetSlot = player.inventory.selectedSlot

        if (slot !is OffHandSlot) {
            player.inventory.selectedSlot = slot.hotbarSlotForServer
            network.sendPacket(UpdateSelectedSlotC2SPacket(slot.hotbarSlotForServer))
        }

        interactItem(slot.useHand, player.yaw, player.pitch)

        sequence.waitTicks(pullback)
        player.stopUsingItem()

        if (slot !is OffHandSlot) {
            resetSlot?.let {
                player.inventory.selectedSlot = it
                network.sendPacket(UpdateSelectedSlotC2SPacket(it))
            }

            resetSlot = null
        }

        isRodUsing = false
    }

    @get:JvmSynthetic
    internal inline val canUseRod
        get() = !isRodUsing && (onItemUsing
                || (player.activeItem?.item != Items.FISHING_ROD
                    && (!player.isUsingItem || !KillAuraAutoBlock.blockVisual)))
}
