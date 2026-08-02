/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isBlockBBValid
import net.ccbluex.liquidbounce.utils.block.center
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.inventorySlot
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getRotationVector
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.minecraft.block.PlantBlock
import net.minecraft.client.options.GameOptions
import net.minecraft.init.Blocks.bed
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.ArmSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMovementActionC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.HitResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.event.ForgeEventFactory
import java.awt.Color

object BedDefender : Module("BedDefender", Category.WORLD) {

    private val autoBlock by choices("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val swing by boolean("Swing", true).subjective()
    private val placeDelay by int("PlaceDelay", 500, 0..1000, suffix = "ms")
    private val raycastMode by choices(
        "Raycast",
        arrayOf("None", "Normal", "Around"),
        "Normal"
    ) { options.rotationsActive }
    private val scannerMode by choices("Scanner", arrayOf("Nearest", "Random"), "Nearest")

    private val options = RotationSettings(this)

    private val onSneakOnly by boolean("OnSneakOnly", true)
    private val autoSneak by choices("AutoSneak", arrayOf("Off", "Normal", "Packet"), "Off") { !onSneakOnly }
    private val trackCPS by boolean("TrackCPS", false)
    private val mark by boolean("Mark", false)

    private val defenceBlocks = mutableListOf<BlockPos>()
    private val bedTopPositions = mutableListOf<BlockPos>()
    private val bedBottomPositions = mutableListOf<BlockPos>()

    private val timerCounter = MSTimer()
    private var position: BlockPos? = null

    override fun onDisable() {
        val player = mc.player ?: return

        if (!GameOptions.isPressed(mc.options.sneakKey)) {
            mc.options.sneakKey.pressed = false
            if (player.isSneaking) player.isSneaking = false
        }

        position = null
        defenceBlocks.clear()
        bedTopPositions.clear()
        bedBottomPositions.clear()
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.player ?: return@handler
        val world = mc.world ?: return@handler

        if (onSneakOnly && !mc.options.sneakKey.isPressed) {
            return@handler
        }

        val radius = 4
        val x = player.x.toInt()
        val y = player.y.toInt()
        val z = player.z.toInt()

        bedTopPositions.clear()
        bedBottomPositions.clear()
        defenceBlocks.clear()

        // Get placing positions
        for (x in x - radius..x + radius) {
            for (y in y - radius..y + radius) {
                for (z in z - radius..z + radius) {
                    val pos = BlockPos(x, y, z)
                    val block = world.getBlockState(pos).block
                    if (block == bed) {
                        val metadata = block.getMetaFromState(world.getBlockState(pos))

                        if (metadata >= 8) {
                            bedTopPositions.add(pos)
                        } else {
                            bedBottomPositions.add(pos)
                        }
                    }
                }
            }
        }

        addDefenceBlocks(bedTopPositions)
        addDefenceBlocks(bedBottomPositions)

        if (defenceBlocks.isNotEmpty()) {
            val playerPos = player.position ?: return@handler
            val pos = if (scannerMode == "Nearest") defenceBlocks.minByOrNull { it.distanceSq(playerPos) }
                ?: return@handler else defenceBlocks.random()
            val pos = BlockPos(pos.x.toDouble(), pos.y - player.eyeHeight + 1.5, pos.z.toDouble())
            val rotation = RotationUtils.toRotation(pos.center, false, player)
            val raytrace = performBlockRaytrace(rotation, mc.interactionManager.blockReachDistance) ?: return@handler

            if (options.rotationsActive) {
                setTargetRotation(rotation, options, if (options.keepRotation) options.resetTicks else 1)
            }

            position = pos

            if (timerCounter.hasTimePassed(placeDelay)) {
                if (!isPlaceablePos(pos)) return@handler

                when (autoSneak) {
                    "Normal" -> mc.options.sneakKey.pressed = false
                    "Packet" -> sendPacket(PlayerMovementActionC2SPacket(player, PlayerMovementActionC2SPacket.Action.START_SNEAKING))
                }

                placeBlock(pos, raytrace.face, raytrace.facePos)
                timerCounter.reset()
            } else {
                when (autoSneak) {
                    "Normal" -> mc.options.sneakKey.pressed = true
                    "Packet" -> sendPacket(PlayerMovementActionC2SPacket(player, PlayerMovementActionC2SPacket.Action.STOP_SNEAKING))
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        if (mark && position != null) {
            val pos = BlockPos(position!!.x, position!!.y + 1, position!!.z)
            drawBlockBox(pos, Color(68, 117, 255, 100), false)
            return@handler
        }
    }

    private fun addDefenceBlocks(bedPositions: List<BlockPos>) {
        for (bedPos in bedPositions) {
            val surroundingPositions = listOf(
                bedPos.up(),
                bedPos.north(),
                bedPos.south(),
                bedPos.east(),
                bedPos.west()
            )

            for (pos in surroundingPositions) {
                if (pos !in bedTopPositions && pos !in bedBottomPositions && mc.world.isAirBlock(pos)) {
                    defenceBlocks.add(pos)
                }
            }
        }
    }

    private fun placeBlock(pos: BlockPos, side: Direction, facePos: Vec3d) {
        val player = mc.player ?: return

        var stack = player.inventorySlot(SilentHotbar.currentSlot + 36).stack ?: return

        if (stack.item !is BlockItem || (stack.item as BlockItem).block is PlantBlock
            || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as BlockItem).block) || stack.size <= 0
        ) {
            val blockSlot = InventoryUtils.findBlockInHotbar() ?: return

            if (autoBlock != "Off") {
                SilentHotbar.selectSlotSilently(
                    this,
                    blockSlot,
                    immediate = true,
                    render = autoBlock == "Pick",
                    resetManually = true
                )
            }

            stack = player.inventorySlot(blockSlot).stack
        }

        tryToPlaceBlock(stack, pos, side, facePos)

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        if (autoBlock == "Switch")
            SilentHotbar.resetSlot(this, true)

        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    private fun tryToPlaceBlock(
        stack: ItemStack,
        clickPos: BlockPos,
        side: Direction,
        facePos: Vec3d,
    ): Boolean {
        val player = mc.player ?: return false

        val prevSize = stack.size

        val clickedSuccessfully = player.onPlayerRightClick(clickPos, side, facePos, stack)

        if (clickedSuccessfully) {
            player.swingArm(!swing)

            if (stack.size <= 0) {
                player.inventory.items[SilentHotbar.currentSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(player, stack)
            } else if (stack.size != prevSize || mc.interactionManager.isInCreativeMode)
                mc.entityRenderer.itemRenderer.resetEquippedProgress()

            position = null
        } else {
            if (player.sendUseItem(stack))
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    private fun isPlaceablePos(pos: BlockPos): Boolean {
        val player = mc.player ?: return false
        val world = mc.world ?: return false

        return when (raycastMode) {
            "Normal" -> {
                val eyesPos = player.eyes
                val movingObjectPosition = world.rayTraceBlocks(eyesPos, pos.center, false, true, false)

                movingObjectPosition != null && movingObjectPosition.pos == pos
            }

            "Around" -> Direction.entries.any { !isBlockBBValid(pos.offset(it)) }

            else -> true
        }
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        if (autoBlock in arrayOf("Off", "Switch") || stack.size > 0)
            return

        val switchSlot = InventoryUtils.findBlockInHotbar() ?: return

        nextTick {
            if (autoBlock != "Off") {
                SilentHotbar.selectSlotSilently(
                    this,
                    switchSlot,
                    immediate = true,
                    render = autoBlock == "Pick",
                    resetManually = true
                )
            }
        }
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): HitResult? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        val eyes = player.eyes
        val rotationVec = getRotationVector(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTraceBlocks(eyes, reach, false, true, false)
    }
}