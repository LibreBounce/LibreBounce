package net.ccbluex.liquidbounce.features.module.modules.combat.elytratarget

import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.anglesmooth.AngleSmooth
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val BASE_YAW_SPEED = 45.0f
private const val BASE_PITCH_SPEED = 35.0f

@Suppress("MagicNumber")
internal open class ElytraRotationsAndAngleSmooth : Configurable("Rotations"), AngleSmooth {
    private val sharpRotations by boolean("Sharp", false)

    private inline val baseYawSpeed: Float get() = if (sharpRotations) {
        BASE_YAW_SPEED * 1.5f
    } else {
        BASE_YAW_SPEED
    }

    private inline val basePitchSpeed: Float get() = if (sharpRotations) {
        BASE_PITCH_SPEED * 1.5f
    } else {
        BASE_PITCH_SPEED
    }

    /**
     * # Sorry, but im TOO LAZY to translate this shit.
     *
     * Адаптивно сглаживает угол, но в то же время
     * позволяет хорошо работать с [ModuleKillAura].
     *
     * Киллаура может просто не бить лол из-за "особых" настроек
     * в виде обязательного убеждения в том, что мы навились на цель.
     *
     * Хорошо поворачивается и работает.
     *
     * Применимо только (и для него сделано) к [ElytraTarget]
     *
     * #### Пожалуйста, не используйте это ГДЕ-ЛИБО ЕЩЕ
     */
    override fun limitAngleChange(
        factorModifier: Float,
        currentRotation: Rotation,
        targetRotation: Rotation,
        vec3d: Vec3d?,
        entity: Entity?
    ): Rotation {
        val delta = currentRotation.rotationDeltaTo(targetRotation)

        val (deltaYaw, deltaPitch) = delta
        val difference = delta.length()

        val currentTime = System.currentTimeMillis()

        val shouldBoost = sin(currentTime / 300.0) > 0.8
        val isTargetBehind = abs(deltaYaw) > 90.0f

        val speedMultiplier = if (shouldBoost) {
            2.0f
        } else {
            1.2f
        }

        val smoothBoost = if (shouldBoost) {
            (sin((currentTime % 360) / 300.0f * Math.PI) * 0.8f + 1.2f).toFloat()
        } else {
            1.2f
        }

        val backTargetMultiplier = if (isTargetBehind) {
            (2.2f * sin(currentTime / 150.0) * 0.2 + 1.0).toFloat()
        } else {
            1.2f
        }

        val speed = speedMultiplier * smoothBoost

        val yawSpeed = baseYawSpeed * speed * backTargetMultiplier
        val pitchSpeed = basePitchSpeed * speed

        val microAdjustment = (sin(currentTime / 80.0) * 0.08 + cos(currentTime / 120.0) * 0.05).toFloat()

        var moveYaw = MathHelper.clamp(deltaYaw, -yawSpeed, yawSpeed)
        var movePitch = MathHelper.clamp(deltaPitch, -pitchSpeed, pitchSpeed)

        if (difference < 5.0f) {
            moveYaw += microAdjustment * 0.2f
            movePitch += microAdjustment * 0.8f
        }

        return Rotation(
            currentRotation.yaw + moveYaw,
            MathHelper.clamp(currentRotation.pitch + movePitch, -90.0f, 90.0f),
        )
    }
}
