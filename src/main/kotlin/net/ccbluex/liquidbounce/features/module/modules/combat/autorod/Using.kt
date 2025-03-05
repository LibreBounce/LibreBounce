package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.Chronometer
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
    private val push by int("Push", 100, 50..1000, suffix = "ms")
    private val pullback by int("Pullback", 500, 50..1000, suffix = "ms")

    internal var isRodUsing = false
    private var resetSlot: Int? = null

    private val pushChronometer = Chronometer()
    private val pullbackChronometer = Chronometer()

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun proceedUsingRod() {
        if (pullbackChronometer.hasElapsed(pullback.toLong())) {
            resetSlot
                ?.takeIf { player.inventory.selectedSlot != it }
                ?.let {
                    player.inventory.selectedSlot = it
                    interaction.syncSelectedSlot()
                }
                ?: run {
                    interaction.stopUsingItem(player)
                }

            resetSlot = null
            isRodUsing = false
            pullbackChronometer.reset()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun startRodUsing(slot: HotbarItemSlot) {
        if (pushChronometer.hasElapsed(push.toLong())) {
            val (yaw, pitch) = RotationManager.currentRotation ?: player.rotation

            interactItem(slot.useHand, yaw, pitch) {
                if (slot !is OffHandSlot && player.activeItem?.item != Items.FISHING_ROD) {
                    resetSlot = slot.hotbarSlotForServer

                    player.inventory.selectedSlot = slot.hotbarSlotForServer
                    interaction.syncSelectedSlot()
                }
            }

            isRodUsing = true
            pushChronometer.reset()
        }
    }

    @get:JvmSynthetic
    internal inline val canUseRodThroughUsingItem
        get() = onItemUsing
                || (player.activeItem?.item != Items.FISHING_ROD
                    && !(player.usingItem || KillAuraAutoBlock.blockVisual))
}
