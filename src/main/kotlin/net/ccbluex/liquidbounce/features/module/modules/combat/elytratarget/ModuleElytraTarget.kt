package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin

private const val IDEAL_DISTANCE = 10

/**
 * Following the target on elytra.
 * Works with [ModuleKillAura] together
 *
 * https://youtu.be/1wa8uKH_apY?si=H84DmdQ2HtvArIPZ
 *
 * @author sqlerrorthing
 */
@Suppress("MagicNumber", "Unused", "UnusedPrivateProperty")
object ModuleElytraTarget : ClientModule("ElytraTarget", Category.COMBAT) {
    private val rotateAt by enumChoice("RotateAt", TargetPosition.EYES)
    internal val targetTracker = tree(TargetTracker())

    private val look by boolean("Look", false)
    private val smartHeight by boolean("SmartHeight", false)
    private val predict by boolean("PredictMovement", true)
    private val autoDistance by boolean("AutoDistance", true)

    init {
        tree(AutoFirework)
    }

    override val running: Boolean
        get() = super.running && player.isGliding

    private inline val randomDirectionVector
        get() = with (System.currentTimeMillis() / 1000.0) {
            Vec3d(
                sin(this * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
                sin(this * 2.2) * 0.03 + (Math.random() - 0.5) * 0.015,
                cos(this * 1.8) * 0.04 + (Math.random() - 0.5) * 0.02,
            )
        }

    @Suppress("unused")
    private val targetUpdate = handler<RotationUpdateEvent> {
        for (target in targetTracker.targets()) {
            val correction = if (look) {
                MovementCorrection.CHANGE_LOOK
            } else {
                MovementCorrection.STRICT
            }

            targetTracker.target = target
            calculateRotation(target).let {
                RotationManager.setRotationTarget(
                    /*
                     * Don't use the RotationConfigurable because I need to superfast rotations.
                     * Without any setting and angle smoothing
                     */
                    plan = RotationTarget(
                        rotation = it,
                        vec3d = it.directionVector,
                        entity = target,
                        angleSmooth = ElytraAdaptiveSmooth,
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
            }

            return@handler
        }

        targetTracker.reset()
    }

    private fun calculateRotation(target: LivingEntity): Rotation {
        var targetPos = if (predict) {
            rotateAt.position(target).add(target.velocity * 2.0)
        } else {
            rotateAt.position(target)
        } + randomDirectionVector * 4.0

        if (smartHeight) {
            (player.y - target.y).takeIf { it < 5 }?.let {
                targetPos = targetPos.add(0.0, 5 - it, 0.0)
            }
        }

        if (autoDistance) {
            val direction = (targetPos - player.pos).normalize()
            val distance = player.pos.squaredDistanceTo(direction)

            if (distance < IDEAL_DISTANCE * IDEAL_DISTANCE) {
                targetPos -= direction * (IDEAL_DISTANCE - distance)
            }
        }

        return Rotation.lookingAt(targetPos, player.pos)
    }

    override fun disable() {
        targetTracker.reset()
    }
}
