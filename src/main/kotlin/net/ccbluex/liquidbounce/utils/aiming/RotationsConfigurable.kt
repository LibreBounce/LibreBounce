package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.anglesmooth.*
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d

/**
 * Configurable to configure the dynamic rotation engine
 */
open class RotationsConfigurable(
    owner: EventListener,
    fixVelocity: Boolean = true,
    changeLook: Boolean = false,
    combatSpecific: Boolean = false
) : Configurable("Rotations") {

    private val angleSmooth = choices(owner, "AngleSmooth", 0) {
        arrayOf(
            LinearAngleSmoothMode(it),
            BezierAngleSmoothMode(it),
            SigmoidAngleSmoothMode(it),
            ConditionalLinearAngleSmoothMode(it),
            AccelerationSmoothMode(it)
        )
    }

    private var slowStart = SlowStart(owner).takeIf { combatSpecific }?.also { tree(it) }
    private var shortStop = ShortStop(owner).takeIf { combatSpecific }?.also { tree(it) }
    private val failFocus = FailFocus(owner).takeIf { combatSpecific }?.also { tree(it) }

    var fixVelocity by boolean("FixVelocity", fixVelocity)
    private val resetThreshold by float("ResetThreshold", 2f, 1f..180f)
    private val ticksUntilReset by int("TicksUntilReset", 5, 1..30, "ticks")
    private val changeLook by boolean("ChangeLook", changeLook)

    fun toAimPlan(rotation: Rotation, vec: Vec3d? = null, entity: Entity? = null,
                  considerInventory: Boolean = false, whenReached: RestrictedSingleUseAction? = null) = AimPlan(
        rotation,
        vec,
        entity,
        angleSmooth.activeChoice,
        slowStart,
        failFocus,
        shortStop,
        ticksUntilReset,
        resetThreshold,
        considerInventory,
        fixVelocity,
        changeLook,
        whenReached
    )

    fun toAimPlan(rotation: Rotation, vec: Vec3d? = null, entity: Entity? = null,
                  considerInventory: Boolean = false, changeLook: Boolean) =
        AimPlan(
            rotation,
            vec,
            entity,
            angleSmooth.activeChoice,
            slowStart,
            failFocus,
            shortStop,
            ticksUntilReset,
            resetThreshold,
            considerInventory,
            fixVelocity,
            changeLook
        )

    /**
     * How long it takes to rotate to a rotation in ticks
     *
     * Calculates the difference from the server rotation to the target rotation and divides it by the
     * minimum turn speed (to make sure we are always there in time)
     *
     * @param rotation The rotation to rotate to
     * @return The amount of ticks it takes to rotate to the rotation
     */
    fun howLongToReach(rotation: Rotation) = angleSmooth.activeChoice
        .howLongToReach(RotationManager.actualServerRotation, rotation)

}
