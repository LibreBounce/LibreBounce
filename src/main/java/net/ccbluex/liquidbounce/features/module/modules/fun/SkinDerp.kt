/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraft.entity.player.EnumPlayerModelParts.*
import kotlin.random.Random.Default.nextBoolean

object SkinDerp : Module("SkinDerp", Category.FUN, subjective = true) {

    private val delay by int("Delay", 0, 0..1000, suffix = "ms")
    private val hat by boolean("Hat", true)
    private val jacket by boolean("Jacket", true)
    private val leftPants by boolean("LeftPants", true)
    private val rightPants by boolean("RightPants", true)
    private val leftSleeve by boolean("LeftSleeve", true)
    private val rightSleeve by boolean("RightSleeve", true)

    private var prevModelParts = emptySet<EnumPlayerModelParts>()

    override fun onEnable() {
        prevModelParts = mc.gameSettings.modelParts

        super.onEnable()
    }

    override fun onDisable() {
        // Disable all current model parts
        for (modelPart in mc.gameSettings.modelParts)
            mc.gameSettings.setModelPartEnabled(modelPart, false)

        // Enable all old model parts
        for (modelPart in prevModelParts)
            mc.gameSettings.setModelPartEnabled(modelPart, true)

        super.onDisable()
    }

    val onUpdate = loopSequence {
        val enableModelPart = mc.gameSettings.setModelPartEnabled

        when {
            hat -> enableModelPart(HAT, nextBoolean())
            jacket -> enableModelPart(JACKET, nextBoolean())
            leftPants -> enableModelPart(LEFT_PANTS_LEG, nextBoolean())
            rightPants -> enableModelPart(RIGHT_PANTS_LEG, nextBoolean())
            leftSleeve -> enableModelPart(LEFT_SLEEVE, nextBoolean())
            rightSleeve -> enableModelPart(RIGHT_SLEEVE, nextBoolean())
        }

        delay(delay.toLong())
    }

}
