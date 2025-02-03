package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.minecraft.util.math.BlockPos
import kotlin.random.Random

object ModuleBlockIn : ClientModule("BlockIn", Category.WORLD) {
    private val blockPlacer = tree(BlockPlacer("BlockPlacer", this, Priority.NORMAL, { this.getSlot() }))
    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val blocks by blocks("Blocks", hashSetOf())
    var startX = -1
    var startZ = -1
    var startY = -65

    private var finalBlockList = mutableListOf<BlockPos>()

    override fun enable() {
        chat("After the block wraps you around on all sides, jump and you'll be completely wrapped in the block")
        startX = player.blockX
        startZ = player.blockZ
        startY = player.blockY

        finalBlockList.clear()

        var baseBlockList = listOf<BlockPos>(
            BlockPos(startX - 1, startY - 1, startZ), BlockPos(startX, startY - 1, startZ + 1),
            BlockPos(startX + 1, startY - 1, startZ), BlockPos(startX, startY - 1, startZ - 1)
        )

        var westBlockList = listOf<BlockPos>(
            BlockPos(startX - 1, startY, startZ), BlockPos(startX - 1, startY + 1, startZ)
        )

        var southBlockList = listOf<BlockPos>(
            BlockPos(startX, startY, startZ - 1), BlockPos(startX, startY + 1, startZ - 1)
        )

        var eastBlockList = listOf<BlockPos>(
            BlockPos(startX + 1, startY, startZ), BlockPos(startX + 1, startY + 1, startZ)
        )

        var northBlockList = listOf<BlockPos>(
            BlockPos(startX, startY, startZ + 1), BlockPos(startX, startY + 1, startZ + 1)
        )

        var topBlockList = listOf<BlockPos>(
            BlockPos(startX - 1, startY + 2, startZ), BlockPos(startX, startY + 2, startZ),
        )

        finalBlockList.addAll(baseBlockList.shuffled())

        var clockwise = Random.nextBoolean()

        if (clockwise) {
            finalBlockList.addAll(eastBlockList)
            finalBlockList.addAll(southBlockList)
            finalBlockList.addAll(westBlockList)
            finalBlockList.addAll(northBlockList)
        } else {
            finalBlockList.addAll(eastBlockList)
            finalBlockList.addAll(northBlockList)
            finalBlockList.addAll(westBlockList)
            finalBlockList.addAll(southBlockList)
        }

        finalBlockList.rotateLeft(2 * Random.nextInt(0, 4))
        finalBlockList.addAll(topBlockList)

    }

    val tickHandler = tickHandler {
        val targetBlock = HashSet<BlockPos>()
        for (i in finalBlockList) {
            if (i.getState()!!.isAir) {
                targetBlock.add(i)
                break
            }
        }
        if (targetBlock.isEmpty()) {
            return@tickHandler
        }
        blockPlacer.update(targetBlock)
    }

    @Suppress("unused")
    private val onmovement = handler<PlayerMovementTickEvent> { event ->
        if (startX == -1 && startZ == -1 && startY == -65) {
            return@handler
        }
        if (player.blockX != startX || player.blockZ != startZ || player.blockY != startY && player.blockY != startY + 1) {
            chat("You postion changed,BlockIn will disable")
            enabled = false
        }
    }

    fun getSlot(): HotbarItemSlot? = filter.getSlot(blocks)

    fun <T> List<T>.rotateLeft(shift: Int): List<T> {
        val s = shift % this.size
        if (s == 0) return this.toList()
        return drop(s) + take(s)
    }

}
