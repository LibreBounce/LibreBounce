/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.rotation

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.ListValue
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.random
import net.ccbluex.liquidbounce.utils.extensions.withGCD
import kotlin.math.abs

// TODO: refactor them all

class AlwaysRotationSettings(owner: Module, generalApply: () -> Boolean = { true }) :
    RotationSettings(owner, generalApply) {
    override val rotationsValue = super.rotationsValue.apply { excludeWithState(true) }
    override val rotationsActive: Boolean = true
}

@Suppress("MemberVisibilityCanBePrivate")
open class RotationSettings(owner: Module, generalApply: () -> Boolean = { true }) : Configurable("RotationSettings") {

    open val rotationsValue = boolean("Rotations", true) { generalApply() }
    open val applyServerSideValue = boolean("ApplyServerSide", true) { rotationsActive && generalApply() }

    open val simulateShortStopValue = boolean("SimulateShortStop", false) { rotationsActive && generalApply() }
    open val rotationDiffBuildUpToStopValue = float("RotationDiffBuildUpToStop", 180f, 50f..720f) { simulateShortStop }
    open val maxThresholdAttemptsToStopValue = int("MaxThresholdAttemptsToStop", 1, 0..5) { simulateShortStop }
    open val shortStopDurationValue = intRange("ShortStopDuration", 1..2, 1..5) { simulateShortStop }

    open val strafeValue = boolean("Strafe", false) { rotationsActive && applyServerSide && generalApply() }
    open val strictValue = boolean("Strict", false) { strafeValue.isActive() && generalApply() }
    open val keepRotationValue = boolean("KeepRotation", true) { rotationsActive && applyServerSide && generalApply() }

    open val resetTicksValue = int("ResetTicks", 1, 1..20) {
        rotationsActive && applyServerSide && generalApply()
    }

    // TODO: Currently, using Scaffold with the Legitimize option causes any consequent rotations to wobble afterwards, unless you use a module like KillAura
    // This should be fixed ASAP
    open val legitimizeValue = boolean("Legitimize", false) { rotationsActive && generalApply() }
    open val legitimizeHorizontalJitterValue = 
        floatRange("LegitimizeHorizontalJitter", -0.03f..0.03f, -1f..1f) { rotationsActive && generalApply() && legitimize }
    open val legitimizeVerticalJitterValue = 
        floatRange("LegitimizeVerticalJitter", -0.02f..0.02f, -1f..1f) { rotationsActive && generalApply() && legitimize }
    open val legitimizeHorizontalSlowdownValue = 
        floatRange("LegitimizeHorizontalSlowdown", 0f..0.1f, 0f..1f) { rotationsActive && generalApply() && legitimize }
    open val legitimizeVerticalSlowdownValue = 
        floatRange("LegitimizeVerticalSlowdown", 0f..0.1f, 0f..1f) { rotationsActive && generalApply() && legitimize }
    open val legitimizeHorizontalImperfectCorrelationFactorValue = 
        floatRange("LegitimizeHorizontalImperfectCorrelationFactor", 0.9f..1.1f, 0f..2f) { rotationsActive && generalApply() && legitimize }
    open val legitimizeVerticalImperfectCorrelationFactorValue = 
        floatRange("LegitimizeVerticalImperfectCorrelationFactor", 0.9f..1.1f, 0f..2f) { rotationsActive && generalApply() && legitimize }

    open val horizontalAngleChangeValue =
        floatRange("HorizontalAngleChange", 180f..180f, 1f..180f) { rotationsActive && generalApply() }
    open val verticalAngleChangeValue =
        floatRange("VerticalAngleChange", 180f..180f, 1f..180f) { rotationsActive && generalApply() }

    open val angleResetDifferenceValue = float("AngleResetDifference", 5f.withGCD(), 0.0f..180f) {
        rotationsActive && applyServerSide && generalApply()
    }

    open val minRotationDifferenceValue = float(
        "MinRotationDifference", 2f, 0f..4f
    ) { rotationsActive && generalApply() }

    open val minRotationDifferenceResetTimingValue = choices(
        "MinRotationDifferenceResetTiming", arrayOf("OnStart", "OnSlowDown", "Always"), "OnStart"
    ) { rotationsActive && generalApply() }

    // Variables for easier access
    val rotations by rotationsValue
    val applyServerSide by applyServerSideValue
    val simulateShortStop by simulateShortStopValue
    val rotationDiffBuildUpToStop by rotationDiffBuildUpToStopValue
    val maxThresholdAttemptsToStop by maxThresholdAttemptsToStopValue
    val shortStopDuration by shortStopDurationValue
    val strafe by strafeValue
    val strict by strictValue
    val keepRotation by keepRotationValue
    val resetTicks by resetTicksValue
    val legitimize by legitimizeValue
    val legitimizeHorizontalJitter by legitimizeHorizontalJitterValue
    val legitimizeVerticalJitter by legitimizeVerticalJitterValue
    val legitimizeHorizontalSlowdown by legitimizeHorizontalSlowdownValue
    val legitimizeVerticalSlowdown by legitimizeVerticalSlowdownValue
    val legitimizeHorizontalImperfectCorrelationFactor by legitimizeHorizontalImperfectCorrelationFactorValue
    val legitimizeVerticalImperfectCorrelationFactor by legitimizeVerticalImperfectCorrelationFactorValue
    val horizontalAngleChange by horizontalAngleChangeValue
    val verticalAngleChange by verticalAngleChangeValue
    val angleResetDifference by angleResetDifferenceValue
    val minRotationDifference by minRotationDifferenceValue
    val minRotationDifferenceResetTiming by minRotationDifferenceResetTimingValue

    var prioritizeRequest = false
    var immediate = false
    var instant = false

    var rotDiffBuildUp = 0f
    var maxThresholdReachAttempts = 0

    open val rotationsActive
        get() = rotations

    val legitimizeHJitter
        get() = legitimizeHorizontalJitter.random()

    val legitimizeVJitter
        get() = legitimizeVerticalJitter.random()

    val legitimizeHSlowdown
        get() = legitimizeHorizontalSlowdown.random()

    val legitimizeVSlowdown
        get() = legitimizeVerticalSlowdown.random()

    val legitimizeHICF
        get() = legitimizeHorizontalImperfectCorrelationFactor.random()

    val legitimizeVICF
        get() = legitimizeVerticalImperfectCorrelationFactor.random()

    val horizontalSpeed
        get() = horizontalAngleChange.random()

    val verticalSpeed
        get() = verticalAngleChange.random()

    fun withoutKeepRotation() = apply {
        keepRotationValue.excludeWithState()
    }

    fun updateSimulateShortStopData(diff: Float) {
        rotDiffBuildUp += diff
    }

    fun resetSimulateShortStopData() {
        rotDiffBuildUp = 0f
        maxThresholdReachAttempts = 0
    }

    fun shouldPerformShortStop(): Boolean {
        if (abs(rotDiffBuildUp) < rotationDiffBuildUpToStop || !simulateShortStop) return false

        if (maxThresholdReachAttempts < maxThresholdAttemptsToStop) {
            maxThresholdReachAttempts++
            return false
        }

        return true
    }

    init {
        owner.addValues(this.values)
    }
}

class RotationSettingsWithRotationModes(
    owner: Module, listValue: ListValue, generalApply: () -> Boolean = { true },
) : RotationSettings(owner, generalApply) {

    override val rotationsValue = super.rotationsValue.apply { excludeWithState() }

    val rotationModeValue = listValue.setSupport { generalApply() }

    val rotationMode by +rotationModeValue

    override val rotationsActive: Boolean
        get() = rotationMode != "Off"
}
