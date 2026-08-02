/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isBlockBBValid
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.onPlayerRightClick
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.extensions.swingArm
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockDamageText
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.performRaytrace
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerHandActionC2SPacket.Action.*
import net.minecraft.network.packet.s2c.play.PlayerMoveS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.awt.Color

object Fucker : Module("Fucker", Category.WORLD) {

    /**
     * SETTINGS
     */
    private val hypixel by boolean("Hypixel", false)

    private val block by block("Block", 26)
    private val throughWalls by choices("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None") { !hypixel }
    private val range by float("Range", 5f, 1f..7f, suffix = "blocks")

    private val action by choices("Action", arrayOf("Destroy", "Use"), "Destroy")
    private val surroundings by boolean("Surroundings", true) { !hypixel }
    private val instant by boolean("Instant", false) { (action == "Destroy" || surroundings) && !hypixel }

    private val switch by int("SwitchDelay", 250, 0..1000, suffix = "ms")
    private val swing by boolean("Swing", true)
    val noHit by boolean("NoHit", false)

    private val options = RotationSettings(this).withoutKeepRotation()

    private val blockProgress by boolean("BlockProgress", true).subjective()

    private val scale by float("Scale", 2f, 1f..6f) { blockProgress }.subjective()
    private val font by font("Font", Fonts.font40) { blockProgress }.subjective()
    private val fontShadow by boolean("Shadow", true) { blockProgress }.subjective()

    private val color by color("Color", Color(200, 100, 0)) { blockProgress }.subjective()

    private val ignoreOwnBed by boolean("IgnoreOwnBed", true)
    private val ownBedDist by int("MaxBedDistance", 16, 1..32, suffix = "blocks") { ignoreOwnBed }

    /**
     * VALUES
     */
    var pos: BlockPos? = null
        private set
    private var obstructingPos: BlockPos? = null
    private var spawnLocation: Vec3d? = null
    private var oldPos: BlockPos? = null
    private var miningCooldown = 0
    private val switchTimer = MSTimer()
    var currentDamage = 0F
    var isOwnBed = false

    // Surroundings
    private var areSurroundings = false

    override fun onToggle(state: Boolean) {
        if (pos != null && !mc.player.abilities.creativeMode) {
            sendPacket(PlayerHandActionC2SPacket(ABORT_DESTROY_BLOCK, pos, Direction.DOWN))
        }

        currentDamage = 0F
        pos = null
        obstructingPos = null
        areSurroundings = false
        isOwnBed = false
    }

    val onPacket = handler<PacketEvent> { event ->
        if (mc.player == null || mc.world == null) return@handler

        val packet = event.packet
        if (packet is PlayerMoveS2CPacket) {
            spawnLocation = Vec3d(packet.x, packet.y, packet.z)
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.player ?: return@handler
        val world = mc.world ?: return@handler

        if (noHit && KillAura.handleEvents() && KillAura.target != null) return@handler

        val targetId = block

        if (pos == null || pos!!.block!!.id != targetId || getCenterDistance(pos!!) > range) {
            pos = find(targetId)
            obstructingPos = null
        }

        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            areSurroundings = false
            isOwnBed = false
            obstructingPos = null
            return@handler
        }

        var currentPos = pos ?: return@handler

        // Check if it is the player's own bed
        isOwnBed = ignoreOwnBed && isBedNearSpawn(currentPos)
        if (isOwnBed) {
            obstructingPos = null
            return@handler
        }

        if (surroundings || hypixel) {
            if (hypixel && obstructingPos == null) {
                val abovePos = currentPos.up()
                if (abovePos.block != air && isHittable(abovePos)) {
                    obstructingPos = abovePos
                    currentPos = obstructingPos!!
                }
            } else if (surroundings && obstructingPos == null) {
                val eyes = player.eyes
                val spotToBed = faceBlock(currentPos) ?: return@handler
                val pos = world.rayTrace(eyes, spotToBed.vec, false, false, true)?.pos
                if (pos != null && pos.block != air && pos != currentPos) {
                    obstructingPos = pos
                    currentPos = obstructingPos!!
                }
            } else if (obstructingPos != null) {
                currentPos = obstructingPos!!
                if (surroundings) {
                    val eyes = player.eyes
                    val spotToObstruction = faceBlock(currentPos) ?: return@handler
                    val rayTraceResultToObstruction = world.rayTrace(eyes, spotToObstruction.vec, false, false, true)
                    // If a new block is blocking it, reset and re-evaluate next cycle.
                    if (rayTraceResultToObstruction?.pos != currentPos &&
                        rayTraceResultToObstruction?.type == net.minecraft.world.HitResult.Type.BLOCK
                    ) {
                        obstructingPos = null
                        return@handler
                    }
                    val spotToBed = faceBlock(pos!!) ?: return@handler
                    val rayTraceToBed = world.rayTrace(eyes, spotToBed.vec, false, false, true)
                    // Target bed if it's open
                    if (rayTraceToBed?.pos == pos &&
                        rayTraceToBed.type == net.minecraft.world.HitResult.Type.BLOCK
                    ) {
                        obstructingPos = null
                        currentPos = pos!!
                    }
                }
            }
        }

        val spot = faceBlock(currentPos) ?: return@handler

        // Reset switch timer when position changes
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
            switchTimer.reset()
        }
        oldPos = currentPos

        if (!switchTimer.hasTimePassed(switch)) return@handler

        // Block hit delay
        if (miningCooldown > 0) {
            miningCooldown--
            return@handler
        }

        // Face block
        if (options.rotationsActive) {
            setTargetRotation(spot.rotation, options = options)
        }
    }

    /**
     * Check if the bed at the given position is near the spawn location
     */
    private fun isBedNearSpawn(currentPos: BlockPos): Boolean {
        if (currentPos.block != Block.byId(block) || spawnLocation == null) {
            return false
        }
        return spawnLocation!!.squareDistanceTo(currentPos.center) < ownBedDist * ownBedDist
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.player ?: return@handler
        val world = mc.world ?: return@handler
        val controller = mc.interactionManager ?: return@handler

        var currentPos = pos ?: return@handler
        if (obstructingPos != null) {
            currentPos = obstructingPos!!
        }

        val targetRotation = if (options.rotationsActive) {
            currentRotation ?: player.rotation
        } else {
            toRotation(currentPos.center, false).fixedSensitivity()
        }

        val raytrace = performRaytrace(currentPos, targetRotation, range) ?: return@handler

        when {
            // Destroy block
            action == "Destroy" || areSurroundings -> {
                isOwnBed = ignoreOwnBed && isBedNearSpawn(currentPos)
                if (isOwnBed) {
                    obstructingPos = null
                    return@handler
                }

                EventManager.call(ClickBlockEvent(currentPos, raytrace.face))

                if (instant && !hypixel) {
                    // CivBreak style block breaking
                    sendPacket(PlayerHandActionC2SPacket(START_DESTROY_BLOCK, currentPos, raytrace.face))
                    if (swing) player.swingArm()
                    sendPacket(PlayerHandActionC2SPacket(STOP_DESTROY_BLOCK, currentPos, raytrace.face))
                    clearTarget(currentPos)
                    return@handler
                }

                val block = currentPos.block ?: return@handler

                if (currentDamage == 0F) {
                    // Prevent flagging FastBreak
                    sendPacket(PlayerHandActionC2SPacket(STOP_DESTROY_BLOCK, currentPos, raytrace.face))
                    nextTick {
                        sendPacket(PlayerHandActionC2SPacket(START_DESTROY_BLOCK, currentPos, raytrace.face))
                    }
                    if (player.abilities.creativeMode ||
                        block.getPlayerRelativeBlockHardness(player, world, currentPos) >= 1f
                    ) {
                        if (swing) player.swingArm()
                        controller.onPlayerDestroyBlock(currentPos, raytrace.face)
                        clearTarget(currentPos)
                        return@handler
                    }
                }

                if (swing) player.swingArm()
                currentDamage += block.getPlayerRelativeBlockHardness(player, world, currentPos)
                world.sendBlockBreakProgress(player.networkId, currentPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    sendPacket(PlayerHandActionC2SPacket(STOP_DESTROY_BLOCK, currentPos, raytrace.face))
                    controller.onPlayerDestroyBlock(currentPos, raytrace.face)
                    miningCooldown = 4
                    clearTarget(currentPos)
                }
            }
            // Use block
            action == "Use" -> {
                if (player.onPlayerRightClick(currentPos, raytrace.face, raytrace.facePos, player.displayItemInHand)) {
                    player.swingArm(!swing)
                    miningCooldown = 4
                    clearTarget(currentPos)
                }
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val offset = obstructingPos ?: pos
        val posToDraw = offset ?: return@handler

        isOwnBed = ignoreOwnBed && isBedNearSpawn(posToDraw)
        if (mc.player == null || isOwnBed) return@handler

        if (block.blockById == air) return@handler

        if (blockProgress) {
            posToDraw.drawBlockDamageText(
                currentDamage,
                font,
                fontShadow,
                color.rgb,
                scale
            )
        }

        drawBlockBox(posToDraw, Color.RED, true)
    }

    /**
     * Finds a new target block by [targetID]
     */
    private fun find(targetID: Int): BlockPos? {
        val eyes = mc.player?.eyes ?: return null
        var nearestBlockDistanceSq = Double.MAX_VALUE
        val nearestBlock = BlockPos.Mutable()
        val rangeSq = range * range

        eyes.getAllInBoxMutable(range + 1.0).forEach {
            val distSq = it.distanceSqToCenter(eyes.xCoord, eyes.yCoord, eyes.zCoord)
            if (it.block?.id != targetID || distSq > rangeSq || distSq > nearestBlockDistanceSq ||
                !isHittable(it) && !surroundings && !hypixel
            ) return@forEach

            nearestBlockDistanceSq = distSq
            nearestBlock.set(it)
        }

        return nearestBlock.takeIf { nearestBlockDistanceSq != Double.MAX_VALUE }
    }

    /**
     * Checks if the block is hittable (or allowed to be hit through walls)
     */
    private fun isHittable(pos: BlockPos): Boolean {
        val player = mc.player ?: return false
        return when (throughWalls) {
            "Raycast" -> {
                val eyesPos = player.eyes
                val movingObjectPosition = mc.world.rayTrace(eyesPos, pos.center, false, true, false)
                movingObjectPosition != null && movingObjectPosition.pos == pos
            }
            "Around" -> Direction.entries.any { !isBlockBBValid(pos.offset(it)) }
            else -> true
        }
    }

    /**
     * Clears the current target if it matches [currentPos] and resets related values.
     */
    private fun clearTarget(currentPos: BlockPos) {
        if (currentPos == obstructingPos) {
            obstructingPos = null
        }
        if (currentPos == pos) {
            pos = null
        }
        areSurroundings = false
        currentDamage = 0F
    }

    override val tag
        get() = getBlockName(block)
}