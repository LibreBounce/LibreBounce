package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.entity.Entity
import net.minecraft.util.math.MathHelper

object RotationUtil {

    val gcd: Double
        get() {
            val f = mc.options.mouseSensitivity.value * 0.6F.toDouble() + 0.2F.toDouble()
            return f * f * f * 8.0 * 0.15F
        }

    /**
     * Calculates the angle between the cross-hair and the entity.
     *
     * Useful for deciding if the player is looking at something or not.
     */
    fun crosshairAngleToEntity(entity: Entity): Float {
        val player = mc.player ?: return 0.0F
        val eyes = player.eyes

        val rotationToEntity = Rotation.lookingAt(point = entity.box.center, from = eyes)

        return player.rotation.angleTo(rotationToEntity)
    }

    /**
     * Calculate difference between two angle points
     */
    fun angleDifference(a: Float, b: Float) = MathHelper.wrapDegrees(a - b)
}
