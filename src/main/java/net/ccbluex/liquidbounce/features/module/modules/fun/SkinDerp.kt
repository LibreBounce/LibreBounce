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
        prevModelParts = mc.options.modelParts

        super.onEnable()
    }

    override fun onDisable() {
        // Disable all current model parts
        for (modelPart in mc.options.modelParts)
            mc.options.setPlayerModelPart(modelPart, false)

        // Enable all old model parts
        for (modelPart in prevModelParts)
            mc.options.setPlayerModelPart(modelPart, true)

        super.onDisable()
    }

    val onUpdate = loopSequence {
        when {
            hat -> mc.options.setPlayerModelPart(HAT, nextBoolean())
            jacket -> mc.options.setPlayerModelPart(JACKET, nextBoolean())
            leftPants -> mc.options.setPlayerModelPart(LEFT_PANTS_LEG, nextBoolean())
            rightPants -> mc.options.setPlayerModelPart(RIGHT_PANTS_LEG, nextBoolean())
            leftSleeve -> mc.options.setPlayerModelPart(LEFT_SLEEVE, nextBoolean())
            rightSleeve -> mc.options.setPlayerModelPart(RIGHT_SLEEVE, nextBoolean())
        }

        delay(delay.toLong())
    }
}
