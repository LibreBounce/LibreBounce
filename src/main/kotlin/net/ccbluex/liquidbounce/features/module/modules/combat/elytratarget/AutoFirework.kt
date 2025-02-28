package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget.ModuleElytraTarget.targetTracker
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.OffHandSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.minecraft.item.Items

/**
 * Initial firework cooldown
 */
@Suppress("MagicNumber")
private var fireworkCooldown = 750

private inline val fireworkSlot
    get() = if (OffHandSlot.itemStack.item == Items.FIREWORK_ROCKET) {
        OffHandSlot
    } else {
        Slots.Hotbar.findSlot(Items.FIREWORK_ROCKET)
    }

private val fireworkChronometer = Chronometer()

internal object AutoFirework : ToggleableConfigurable(ModuleElytraTarget, "AutoFirework", true) {
    private val useMode by enumChoice("UseMode", FireworkUseMode.PACKET)
    private val extraDistance by float("ExtraDistance", 50f, 5f..100f, suffix = "m")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    private val cooldown by intRange("Cooldown", 8..10, 1..50, "ticks")

    @Suppress("unused")
    private val autoFireworkHandler = tickHandler {
        val target = targetTracker.target ?: return@tickHandler

        if (fireworkChronometer.hasElapsed((fireworkCooldown * 50).toLong())) {
            val slot = fireworkSlot ?: return@tickHandler
            useMode.useFireworkSlot(slot, slotResetDelay.random())
            fireworkChronometer.reset()
        }

        val distance = extraDistance

        fireworkCooldown = if (target.squaredBoxedDistanceTo(player) > distance * distance) {
            cooldown.max()
        } else {
            cooldown.min()
        }
    }
}
