package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
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
import net.ccbluex.liquidbounce.utils.render.WorldTargetRenderer
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
@Suppress("MagicNumber")
object ModuleElytraTarget : ClientModule("ElytraTarget", Category.COMBAT) {
    internal val targetTracker = tree(TargetTracker())
    private val targetRenderer = tree(WorldTargetRenderer(this))

    private val rotations = tree(object : ElytraRotationsAndAngleSmooth() {
        val ignoreKillAura by boolean("IgnoreKillAuraRotation", false)
        val look by boolean("Look", false)
        val autoDistance by boolean("AutoDistance", true)
        val rotateAt by enumChoice("RotateAt", TargetRotatePosition.EYES)
    })

    val canIgnoreKillAuraRotations get() =
        running
        && rotations.ignoreKillAura

    fun isSameTargetRendering(target: LivingEntity) =
        running
        && targetRenderer.enabled
        && targetTracker.target
            ?.takeIf { it == target } != null

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
    private val renderTargetHandler = handler<WorldRenderEvent> { event ->
        val target = targetTracker.target
            ?.takeIf { targetRenderer.enabled }
            ?: return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            targetRenderer.render(this, target, event.partialTicks)
        }
    }

    @Suppress("unused")
    private val targetUpdate = handler<RotationUpdateEvent> {
        val target = targetTracker.selectFirst { potentialTarget ->
            player.canSee(potentialTarget)
        } ?: return@handler

        val correction = if (rotations.look) {
            MovementCorrection.CHANGE_LOOK
        } else {
            MovementCorrection.STRICT
        }

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
                    angleSmooth = rotations,
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
    }

    private fun calculateRotation(target: LivingEntity): Rotation {
        var targetPos = rotations.rotateAt.position(target) + randomDirectionVector * 4.0

        if (rotations.autoDistance) {
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
