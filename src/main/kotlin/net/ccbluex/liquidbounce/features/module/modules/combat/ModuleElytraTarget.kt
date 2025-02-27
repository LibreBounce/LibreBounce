package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.item.Items
import kotlin.math.atan2
import kotlin.math.sqrt

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
@Suppress("MagicNumber", "Unused", "UnusedPrivateProperty")
object ModuleElytraTarget : ClientModule("ElytraTarget", Category.COMBAT) {
    private val autoFirework = tree(object : ToggleableConfigurable(this, "AutoFirework", true) {
        val extraDistance by float("ExtraDistance", 50f, 5f..100f, suffix = "m")
        val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..20, "ticks")
    })

    private val look by boolean("Look", false)
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

            val (x, y, z) = target.eyePos - player.pos

            val rotation = Rotation(
                (Math.toDegrees(atan2(z, x)) - 90).toFloat(),
                (-Math.toDegrees(atan2(y, sqrt(x * x + z * z)))).toFloat()
            ).normalize()

            val correction = if (look) {
                MovementCorrection.CHANGE_LOOK
            } else {
                MovementCorrection.STRICT
            }

            targetTracker.target = target
            RotationManager.setRotationTarget(
                /*
                 * Don't use the RotationConfigurable because I need to superfast rotations.
                 * Without any setting and angle smoothing
                 */
                plan = RotationTarget(
                    rotation = rotation,
                    vec3d = rotation.directionVector,
                    entity = target,
                    angleSmooth = null,
                    slowStart = null,
                    failFocus = null,
                    shortStop = null,
                    ticksUntilReset = 1,
                    resetThreshold = 1f,
                    considerInventory = true,
                    movementCorrection = correction
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
