@file:Suppress("MaxLineLength")
package net.ccbluex.liquidbounce.features.module.modules.combat.autorod

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.Items

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

    private inline val usingRod
        get() = (player.isUsingItem
                && player.activeItem?.item == Items.FISHING_ROD)
                || using.isUsingRod

    private inline val testFacingEnemyUseRod
        get() = facingEnemy.enabled
                && player.health >= playerHealthThreshold.toFloat()
                && facingEnemy.testUseRod()

    private inline val canUseRod get() = when {
        usingRod -> false
        player.health <= escapeHealthThreshold -> true
        !facingEnemy.enabled -> true
        else -> testFacingEnemyUseRod
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (canUseRod) {
            Slots.OffhandWithHotbar.findSlot(Items.FISHING_ROD)
                ?.takeIf { using.canUseRodWhenUsingItem }
                ?.let {
                    using.startRodUsing(it)
                }
        } else if (using.isUsingRod) {
            using.proceedUsingRod()
        }
    }

    override fun disable() {
        using.isUsingRod = false
    }
}
