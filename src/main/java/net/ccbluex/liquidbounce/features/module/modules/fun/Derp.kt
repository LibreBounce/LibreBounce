/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.syncSpecialModuleRotations

object Derp : Module("Derp", Category.FUN, subjective = true) {

    private val headless by boolean("Headless", false)
    private val spinny by boolean("Spinny", false)
    private val increment by float("Increment", 1F, 0F..50F) { spinny }

    override fun onDisable() {
        syncSpecialModuleRotations()
    }

    val rotation: Rotation
        get() {
            val rotationToUse = currentRotation ?: serverRotation

            val rot = Rotation(rotationToUse.yaw, nextFloat(-90f, 90f))

            if (headless)
                rot.pitch = 180F

            rot.yaw += if (spinny) increment else nextFloat(-180f, 180f)

            return rot.fixedSensitivity()
        }

}