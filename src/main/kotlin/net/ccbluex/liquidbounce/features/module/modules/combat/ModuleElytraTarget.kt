package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.getSpot
import net.ccbluex.liquidbounce.utils.aiming.PointTracker
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.item.Items

private var fireworkCooldown = 750L

/**
 * Выбирает цель и летит за ней на элитре.
 * Работает в паре с киллаурой
 *
 * https://youtu.be/1wa8uKH_apY?si=H84DmdQ2HtvArIPZ
 *
 * @author sqlerrorthing
 * @see ModuleKillaura.rotations
 */
@Suppress("MagicNumber", "NOTHING_TO_INLINE", "Unused", "UnusedPrivateProperty")
object ModuleElytraTarget : ClientModule("ElytraTarget", Category.COMBAT) {
    private val autoFirework = tree(object : ToggleableConfigurable(this, "AutoFirework", true) {
        val extraDistance by float("ExtraDistance", 50f, 5f..100f, suffix = "m")
        val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    })

    private val rotations = tree(RotationsConfigurable(this, MovementCorrection.STRICT))
    private val targetTracker = tree(TargetTracker())

    override val running: Boolean
        get() = super.running && player.isGliding

    private inline val range get() = targetTracker.maxRange

    private inline val fireworkSlot
        get() = if (OffHandSlot.itemStack.item == Items.FIREWORK_ROCKET) {
            OffHandSlot
        } else {
            Slots.Hotbar.findSlot(Items.FIREWORK_ROCKET)
        }

    private val fireworkChronometer = Chronometer()

    @Suppress("unused")
    private val targetUpdate = handler<RotationUpdateEvent> {
        for (target in targetTracker.targets()) {
            if (target.squaredBoxedDistanceTo(player) > range * range) {
                continue
            }

            val spot = getSpot(target, range.toDouble(), PointTracker.AimSituation.FOR_NOW, false) ?: continue
            val (rotation, vec) = spot

            targetTracker.target = target
            RotationManager.setRotationTarget(
                rotations.toAimPlan(
                    rotation,
                    vec,
                    target,
                    considerInventory = true
                ),
                priority = Priority.IMPORTANT_FOR_USAGE_3,
                provider = this
            )

            return@handler
        }

        targetTracker.reset()
    }

    @Suppress("unused")
    private val autoFireworkHandler = tickHandler {
        val target = targetTracker.target ?: return@tickHandler

        if (!autoFirework.enabled) {
            return@tickHandler
        }

        if (fireworkChronometer.hasElapsed(fireworkCooldown)) {
            val slot = fireworkSlot ?: return@tickHandler
            useHotbarSlotOrOffhand(slot, autoFirework.slotResetDelay.random())

            fireworkChronometer.reset()
        }

        val distance = autoFirework.extraDistance

        fireworkCooldown = if (target.squaredBoxedDistanceTo(player) > distance * distance) {
            300
        } else {
            200
        }
    }

    override fun disable() {
        targetTracker.reset()
    }
}
