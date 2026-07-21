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
import net.minecraft.client.render.model.PlayerModelPart
import net.minecraft.client.render.model.PlayerModelPart.*
import kotlin.random.Random.Default.nextBoolean

object SkinDerp : Module("SkinDerp", Category.FUN, subjective = true) {

    private val delay by int("Delay", 0, 0..1000, suffix = "ms")
    private val hat by boolean("Hat", true)
    private val jacket by boolean("Jacket", true)
    private val leftPants by boolean("LeftPants", true)
    private val rightPants by boolean("RightPants", true)
    private val leftSleeve by boolean("LeftSleeve", true)
    private val rightSleeve by boolean("RightSleeve", true)

    private var prevModelParts = emptySet<PlayerModelPart>()

    override fun onEnable() {
        prevModelParts = mc.gameOptions.modelParts

        super.onEnable()
    }

    override fun onDisable() {
        // Disable all current model parts
        for (modelPart in mc.gameOptions.modelParts)
            mc.gameOptions.setModelPartEnabled(modelPart, false)

        // Enable all old model parts
        for (modelPart in prevModelParts)
            mc.gameOptions.setModelPartEnabled(modelPart, true)

        super.onDisable()
    }

    val onUpdate = loopSequence {
        when {
            hat -> mc.gameOptions.setModelPartEnabled(HAT, nextBoolean())
            jacket -> mc.gameOptions.setModelPartEnabled(JACKET, nextBoolean())
            leftPants -> mc.gameOptions.setModelPartEnabled(LEFT_PANTS_LEG, nextBoolean())
            rightPants -> mc.gameOptions.setModelPartEnabled(RIGHT_PANTS_LEG, nextBoolean())
            leftSleeve -> mc.gameOptions.setModelPartEnabled(LEFT_SLEEVE, nextBoolean())
            rightSleeve -> mc.gameOptions.setModelPartEnabled(RIGHT_SLEEVE, nextBoolean())
        }

        delay(delay.toLong())
    }
}
