@file:Suppress("MaxLineLength")
package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.Items

@get:JvmSynthetic
private inline val rodSlot
    get() = if (OffHandSlot.itemStack.item == Items.FISHING_ROD) {
        OffHandSlot
    } else {
        Slots.Hotbar.findSlot(Items.FISHING_ROD)
    }

/**
 * Auto use fishing rod to PVP.
 *
 * The original code was taken from [LiquidBounce Legacy](https://github.com/CCBlueX/LiquidBounce/blob/legacy/src/main/java/net/ccbluex/liquidbounce/features/module/modules/combat/AutoRod.kt).
 *
 * @author zyklone4096, sqlerrorthing
 */
@Suppress("MagicNumber")
object ModuleAutoRod : ClientModule("AutoRod", Category.COMBAT) {
    private val facingEnemy = tree(FacingEnemy())
    private val using = tree(Using())

    private val playerHealthThreshold by int("PlayerHealthThreshold", 5, 1..20)
    private val escapeHealthThreshold by int("EscapeHealthThreshold", 10, 1..20)

    override val running: Boolean
        get() = super.running

    @get:JvmSynthetic
    private inline val usingRod
        get() = (player.isUsingItem && player.activeItem?.item == Items.FISHING_ROD) || using.isRodUsing

    @get:JvmSynthetic
    private inline val canUseRod: Boolean get() {
        return if (usingRod) {
            false
        } else if (facingEnemy.enabled && player.health >= playerHealthThreshold.toFloat() && facingEnemy.testUseRod()) {
            true
        } else if (player.health <= escapeHealthThreshold) {
            true
        } else if (!facingEnemy.enabled) {
            true
        } else {
            false
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!canUseRod) {
            if (using.isRodUsing) {
                using.proceedUsingRod()
            }
        } else if (using.canUseRodThroughUsingItem) {
            rodSlot?.let {
                using.startRodUsing(it)
            }
        }
    }

    override fun disable() {
        using.isRodUsing = false
    }
}
