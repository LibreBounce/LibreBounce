/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.choices
import net.ccbluex.liquidbounce.config.int
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.loopHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.minecraft.block.Block
import net.minecraft.block.Block.getIdFromBlock
import net.minecraft.init.Blocks.*
import net.minecraft.util.BlockPos
import java.awt.Color

object BedProtectionESP : Module("BedProtectionESP", Category.RENDER, hideModule = false) {
    private val targetBlock by choices("TargetBlock", arrayOf("Bed", "DragonEgg"), "Bed")
    private val renderMode by choices("LayerRenderMode", arrayOf("Current", "All"), "Current")
    private val radius by int("Radius", 8, 0..32)
    private val maxLayers by int("MaxProtectionLayers", 2, 1..6)
    private val blockLimit by int("BlockLimit", 256, 0..1024)
    private val down by boolean("BlocksUnderTarget", false)
    private val renderTargetBlocks by boolean("RenderTargetBlocks", true)

    private val colorRainbow by boolean("Rainbow", false)
    private val colorRed by int("R", 96, 0..255) { !colorRainbow }
    private val colorGreen by int("G", 96, 0..255) { !colorRainbow }
    private val colorBlue by int("B", 96, 0..255) { !colorRainbow }

    private var targetBlocks = emptySet<BlockPos>()
    private var blocksToRender = emptySet<BlockPos>()

    private val breakableBlockIDs =
        arrayOf(35, 24, 159, 121, 20, 5, 49) // wool, sandstone, stained_clay, end_stone, glass, wood, obsidian

    private fun getBlocksToRender(
        targetBlock: Block,
        maxLayers: Int,
        down: Boolean,
        allLayers: Boolean,
        blockLimit: Int
    ): Set<BlockPos> {
        val result = hashSetOf<BlockPos>()
        val targetBlockID = getIdFromBlock(targetBlock)

        val nextLayerAirBlocks = mutableSetOf<BlockPos>()
        val nextLayerBlocks = mutableSetOf<BlockPos>()
        val cachedBlocks = mutableSetOf<BlockPos>()
        val currentLayerBlocks = ArrayDeque<BlockPos>()
        var currentLayer = 1

        // get blocks around each target block
        for (block in targetBlocks) {
            currentLayerBlocks.add(block)

            while (currentLayerBlocks.isNotEmpty()) {
                val currBlock = currentLayerBlocks.removeFirst()
                val currBlockID = getIdFromBlock(currBlock.block)

                // it's not necessary to make protection layers around unbreakable blocks
                if (breakableBlockIDs.contains(currBlockID) || (currBlockID == targetBlockID) || (allLayers && currBlockID == 0)) {
                    val blocksAround = mutableListOf(
                        currBlock.north(),
                        currBlock.east(),
                        currBlock.south(),
                        currBlock.west(),
                        currBlock.up(),
                    )

                    if (down) {
                        blocksAround.add(currBlock.down())
                    }

                    blocksAround.filterTo(nextLayerAirBlocks) { blockPos -> blockPos.block == air }

                    blocksAround.filterTo(nextLayerBlocks) { blockPos ->
                        (allLayers || blockPos.block != air) && !cachedBlocks.contains(
                            blockPos
                        )
                    }
                }

                // move to the next layer
                if (currentLayerBlocks.isEmpty() && (allLayers || nextLayerAirBlocks.isEmpty()) && currentLayer < maxLayers) {
                    currentLayerBlocks += nextLayerBlocks
                    cachedBlocks += nextLayerBlocks
                    nextLayerBlocks.clear()
                    currentLayer += 1
                }
            }

            nextLayerBlocks.clear()
            cachedBlocks.clear()
            currentLayer = 1

            for (newBlock in nextLayerAirBlocks) {
                if (result.size >= blockLimit) {
                    return result
                }

                result += newBlock
            }

            nextLayerAirBlocks.clear()
        }

        return result
    }

    val onSearch = loopHandler(dispatcher = Dispatchers.Default) {
        val radius = radius
        val targetBlock = if (targetBlock == "Bed") bed else dragon_egg
        val maxLayers = maxLayers
        val down = down
        val allLayers = renderMode == "All"
        val blockLimit = blockLimit

        val blocks = searchBlocks(radius, setOf(targetBlock), 32)

        withContext(Dispatchers.Main) {
            targetBlocks = blocks.keys
            blocksToRender = getBlocksToRender(targetBlock, maxLayers, down, allLayers, blockLimit)
        }

        delay(1000)
    }

    val onRender3D = handler<Render3DEvent> {
        if (renderTargetBlocks) {
            for (blockPos in targetBlocks) {
                drawBlockBox(blockPos, Color.RED, true)
            }
        }

        val color = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)
        for (blockPos in blocksToRender) {
            drawBlockBox(blockPos, color, true)
        }
    }

    override fun onDisable() {
        targetBlocks = emptySet()
        blocksToRender = emptySet()
    }

    override val tag: String
        get() = blocksToRender.size.toString()
}