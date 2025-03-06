package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.interactItem
import net.minecraft.item.Items

@Suppress("MagicNumber")
internal class Using : Configurable("Using") {
    private val onItemUsing by boolean("IgnoreUsingItem", false)

    private val push = tree(Push())
    private val pullback = tree(Pullback())

    internal var isUsingRod = false
    private var resetSlot: Int? = null

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun startRodUsing(slot: HotbarItemSlot) {
        push.testPushRod {
            val (yaw, pitch) = RotationManager.currentRotation ?: player.rotation

            interactItem(slot.useHand, yaw, pitch) {
                slot
                    .takeIf { it !is OffHandSlot }?.hotbarSlotForServer
                    ?.takeIf { it != player.inventory.selectedSlot }
                    ?.let {
                        resetSlot = player.inventory.selectedSlot

                        player.inventory.selectedSlot = it
                        interaction.syncSelectedSlot()
                    }
            }

            isUsingRod = true
            pullback.reset()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun proceedUsingRod() {
        pullback.testPullbackRod {
            if (canUseRodWhenUsingItem) {
                interaction.stopUsingItem(player)

                resetSlot
                    ?.takeIf { player.inventory.selectedSlot != it }
                    ?.let {
                        player.inventory.selectedSlot = it
                        interaction.syncSelectedSlot()
                    }
            }

            resetSlot = null
            isUsingRod = false
            push.reset()
        }
    }

    @get:JvmSynthetic
    internal inline val canUseRodWhenUsingItem
        get() = onItemUsing
                || (player.activeItem?.item != Items.FISHING_ROD
                    && !(player.usingItem || KillAuraAutoBlock.blockVisual))
}
