/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.config.block
import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.choices
import net.ccbluex.liquidbounce.config.int
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.loopHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

object BlockESP : Module("BlockESP", Category.RENDER, hideModule = false) {
    private val mode by choices("Mode", arrayOf("Box", "2D"), "Box")
    private val block by block("Block", 168)
    private val radius by int("Radius", 40, 5..120)
    private val blockLimit by int("BlockLimit", 256, 0..2056)

    private val colorRainbow by boolean("Rainbow", false)
    private val colorRed by int("R", 255, 0..255) { !colorRainbow }
    private val colorGreen by int("G", 179, 0..255) { !colorRainbow }
    private val colorBlue by int("B", 72, 0..255) { !colorRainbow }

    private val posList = ConcurrentHashMap.newKeySet<BlockPos>()

    override fun onDisable() {
        posList.clear()
    }

    val onUpdate = loopHandler(dispatcher = Dispatchers.Default) {
        val selectedBlock = Block.getBlockById(block)

        if (selectedBlock == null || selectedBlock == air)
            return@loopHandler

        posList.removeIf {
            it.block != selectedBlock
        }

        posList += searchBlocks(radius, setOf(selectedBlock), blockLimit).keys

        delay(1000)
    }

    val onRender3D = handler<Render3DEvent> {
        val color = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)
        when (mode) {
            "Box" -> posList.forEach { drawBlockBox(it, color, true) }
            "2D" -> posList.forEach { draw2D(it, color.rgb, Color.BLACK.rgb) }
        }
    }

    override val tag
        get() = getBlockName(block)
}