/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.other

import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.autoMLG
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.currentMlgBlock
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.maxRetrievalWaitingTime
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.options
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.retrieveDelay
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.retrievingPos
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.NoFall.swing
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallMode
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.block.center
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.inventory.inventorySlot
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getRotationVector
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.minecraft.init.Blocks.web
import net.minecraft.init.Blocks.water
import net.minecraft.item.Items
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.util.*
import net.minecraftforge.event.ForgeEventFactory
import kotlin.math.min

object MLG : NoFallMode("MLG") {

    private val mlgSlot
        get() = findMlgSlot()

    private val currRotation
        get() = RotationUtils.currentRotation ?: RotationUtils.serverRotation

    override fun onRotationUpdate() {
        val player = mc.player ?: return

        retrievingPos?.let {
            if (player.hotBarSlot(SilentHotbar.currentSlot).stack?.item != Items.bucket) {
                retrievingPos = null
                return@let
            }

            RotationUtils.setTargetRotation(
                toRotation(it),
                options,
                if (options.keepRotation) options.resetTicks else 1
            )
        }

        mlgSlot ?: return

        currentMlgBlock = null

        val reach = mc.playerController.blockReachDistance

        if (player.fallDistance >= NoFall.minFallDistance) {
            SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput).let { sim ->
                sim.rotationYaw = currRotation.yaw

                var suitablePos: BlockPos? = null

                for (i in 1..40) {
                    sim.tick()

                    val pos = BlockPos(sim.pos).down()

                    if (sim.fallDistance == 0F) {
                        var bestOffset: Vec3i? = null
                        var minDistance = Double.MAX_VALUE

                        for (x in -1..1) {
                            for (z in -1..1) {
                                val offset = Vec3i(x, 0, z)
                                val neighbor = pos.add(offset)
                                val center = neighbor.center

                                val raytrace = Backtrack.runWithSimulatedPosition(player, sim.pos) {
                                    performBlockRaytrace(toRotation(center), reach)
                                }

                                if (raytrace?.let { it.blockPos == neighbor && it.sideHit == Direction.UP } == true) {
                                    val distance = BlockPos(sim.pos).distanceSq(neighbor)

                                    if (distance <= minDistance) {
                                        minDistance = distance
                                        bestOffset = offset
                                    }
                                }
                            }
                        }

                        bestOffset?.let {
                            suitablePos = pos.add(it)

                            if (suitablePos?.state?.block == web ||
                                suitablePos?.up()?.block == water
                            ) {
                                return
                            }
                        }

                        if (suitablePos != null) {
                            break
                        }
                    }
                }

                suitablePos
            }?.also { currentMlgBlock = it }?.let { pos ->
                // The higher the fall distance, the greater the focus to the center of the block
                val inc = 0.2 * min(player.fallDistance / 30F, 1F)

                faceBlock(pos, targetUpperFace = true, hRange = 0.3 + inc..0.701 - inc)?.run {
                    RotationUtils.setTargetRotation(
                        rotation, options, if (options.keepRotation) options.resetTicks else 1
                    )
                }
            }

        }
    }

    override fun onTick() {
        val player = mc.player ?: return
        val target = currentMlgBlock ?: run {
            // If the slot was modified but rotations did not reach the target spot in time, reset the slot
            if (retrievingPos == null) {
                SilentHotbar.resetSlot(this)
            }

            return
        }

        val reach = mc.playerController.blockReachDistance

        val stack = mlgSlot?.let {
            if (retrievingPos != null) return@let null

            SilentHotbar.selectSlotSilently(this, it, render = autoMLG == "Pick", resetManually = true)

            player.hotBarSlot(it).stack
        } ?: return

        val item = stack.item ?: return

        val wasWaterBucket = item == Items.water_bucket

        if (wasWaterBucket || (item as? BlockItem)?.block == web) {
            performBlockRaytrace(currRotation, reach)?.let {
                if (it.blockPos != target || it.sideHit != Direction.UP) {
                    return@let
                }

                placeBlock(it.blockPos, it.sideHit, it.hitVec, stack, !wasWaterBucket) {
                    if (!wasWaterBucket) {
                        currentMlgBlock = null
                        retrievingPos = null
                    }
                }

                if (wasWaterBucket) {
                    val placePos = target.center.withY(0.5, true)

                    retrievingPos = placePos

                    WaitTickUtils.conditionalSchedule(this, maxRetrievalWaitingTime) { elapsedTicks ->
                        val newStack =
                            player.hotBarSlot(SilentHotbar.currentSlot).stack ?: return@conditionalSchedule null

                        if (newStack.item == Items.bucket) {
                            findMlgSlot(true)?.let { slot ->
                                SilentHotbar.selectSlotSilently(
                                    this, slot, render = autoMLG == "Pick", resetManually = true
                                )
                            } ?: run {
                                reset()

                                return@conditionalSchedule null
                            }
                        }

                        val block = target.state?.block

                        // Are we too far away from the block?
                        if (block == null || player.getDistanceToBox(
                                block.getSelectedBoundingBox(mc.world, target)
                            ) > reach
                        ) {
                            reset()

                            return@conditionalSchedule null
                        }

                        if (player.fallDistance == 0F) {
                            val raytrace = performBlockRaytrace(currRotation, reach)
                            // Did the user decide to look somewhere else?
                            if (raytrace == null || raytrace.blockPos != target || raytrace.sideHit != Direction.UP) {
                                // Reset the rotation if it took more than the max retrieval waiting time to retrieve
                                reset(elapsedTicks >= maxRetrievalWaitingTime)
                                return@conditionalSchedule null
                            }

                            // We are looking at the target block, now make sure the time has passed to retrieve
                            if (elapsedTicks < retrieveDelay) return@conditionalSchedule false

                            // Time to retrieve
                            placeBlock(it.blockPos, it.sideHit, it.hitVec, newStack)

                            reset()

                            return@conditionalSchedule true
                        }

                        return@conditionalSchedule false
                    }
                }
            }
        }
    }

    private fun reset(complete: Boolean = true) {
        // Reset target information
        currentMlgBlock = null

        if (complete) {
            retrievingPos = null

            SilentHotbar.resetSlot(this)
        }
    }

    private inline fun placeBlock(
        blockPos: BlockPos,
        side: Direction,
        hitVec: Vec3d,
        stack: ItemStack,
        finalStage: Boolean = true,
        onSuccess: () -> Unit = { }
    ) {
        tryToPlaceBlock(stack, blockPos, side, hitVec, onSuccess)

        if (finalStage) {
            switchBlockNextTickIfPossible(stack)
        }
    }

    private inline fun tryToPlaceBlock(
        stack: ItemStack, clickPos: BlockPos, side: Direction, hitVec: Vec3d, onSuccess: () -> Unit
    ): Boolean {
        val player = mc.player ?: return false

        val prevSize = stack.stackSize

        val clickedSuccessfully = player.onPlayerRightClick(clickPos, side, hitVec, stack)

        if (clickedSuccessfully) {
            player.swingItem(!swing)

            if (stack.stackSize <= 0) {
                player.inventory.mainInventory[SilentHotbar.currentSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(player, stack)
            } else if (stack.stackSize != prevSize || mc.playerController.isInCreativeMode) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress()
            }

            onSuccess()
        } else {
            if (player.sendUseItem(stack)) {
                mc.entityRenderer.itemRenderer.resetEquippedProgress2()

                onSuccess()
            }
        }

        return clickedSuccessfully
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        if (autoMLG == "Off" || stack.stackSize > 0) return

        val switchSlot = findMlgSlot() ?: return

        SilentHotbar.selectSlotSilently(this, switchSlot, render = autoMLG == "Pick", resetManually = true)
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): HitResult? {
        val player = mc.player ?: return null
        mc.world ?: return null

        val eyes = player.eyes
        val rotationVec = getRotationVector(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return mc.world.rayTraceBlocks(eyes, reach, false, true, false)
    }

    private fun findMlgSlot(onlyBucket: Boolean = false): Int? {
        val player = mc.player ?: return null

        val bucket = if (onlyBucket) Items.bucket else Items.water_bucket

        player.hotBarSlot(SilentHotbar.currentSlot).stack?.item.let {
            // Already have required item? Why change slot?
            if (it == bucket || (it as? BlockItem)?.block in arrayOf(web)) {
                return SilentHotbar.currentSlot
            }
        }

        for (i in 36..44) {
            val item = player.inventorySlot(i).stack?.item ?: continue

            if (item == bucket || !onlyBucket && (item as? BlockItem)?.block in arrayOf(web)) {
                return i - 36
            }
        }

        return null
    }
}