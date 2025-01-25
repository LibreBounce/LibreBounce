/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.loopHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.player.EnumPlayerModelParts
import kotlin.random.Random.Default.nextBoolean

object SkinDerp : Module("SkinDerp", Category.FUN, subjective = true) {

    private val delay by int("Delay", 0, 0..1000)
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

    val onUpdate = loopHandler {
        if (hat)
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.HAT, nextBoolean())
        if (jacket)
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.JACKET, nextBoolean())
        if (leftPants)
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.LEFT_PANTS_LEG, nextBoolean())
        if (rightPants)
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.RIGHT_PANTS_LEG, nextBoolean())
        if (leftSleeve)
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.LEFT_SLEEVE, nextBoolean())
        if (rightSleeve)
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.RIGHT_SLEEVE, nextBoolean())

        delay(delay.toLong())
    }

}
