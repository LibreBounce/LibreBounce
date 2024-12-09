/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.packetmine

import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.BlockAttackEvent
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoTool
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.CivMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.ImmediateMineMode
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.mode.NormalMineMode
import net.ccbluex.liquidbounce.render.EMPTY_BOX
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.getCenterDistanceSquaredEyes
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.outlineBox
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.block.BlockState
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectUtil
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.registry.tag.FluidTags
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import kotlin.math.max

/**
 * PacketMine module
 *
 * Automatically mines blocks you click once. Using AutoTool is recommended.
 *
 * @author ccetl
 */
@Suppress("TooManyFunctions")
object ModulePacketMine : ClientModule("PacketMine", Category.WORLD) {

    val mode = choices<MineMode>(
        this,
        "Mode",
        NormalMineMode,
        arrayOf(NormalMineMode, ImmediateMineMode, CivMineMode)
    )

    init {
        mode.onChanged {
            if (mc.world != null && mc.player != null) {
                disable()
                enable()
            }
        }
    }

    private val range by float("Range", 4.5f, 1f..6f)
    private val wallsRange by float("WallsRange", 4.5f, 0f..6f).onChange {
        it.coerceAtLeast(range)
    }
    private val keepRange by float("KeepRange", 25f, 0f..200f).onChange {
        it.coerceAtLeast(wallsRange)
    }
    val swingMode by enumChoice("Swing", SwingMode.HIDE_CLIENT)
    private val switchMode by enumChoice("Switch", ToolMode.ON_STOP)
    private val rotationMode by enumChoice("Rotate", RotationMode.NEVER)
    private val rotationsConfigurable = tree(RotationsConfigurable(this))
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val targetRenderer = tree(
        PlacementRenderer(
            "TargetRendering", true, this,
            defaultColor = Color4b(255, 255, 0, 90),
            clump = false
        )
    )

    private val chronometer = Chronometer()

    var finished = false
    var progress = 0f
    private var direction: Direction? = null
    private var started = false
    private var shouldRotate = rotationMode.start
    private var targetPos: BlockPos? = null
        set(value) {
            if (value == field) {
                return
            }

            field?.let {
                targetRenderer.removeBlock(it)
                if (!finished && mode.activeChoice.canAbort) {
                    abort(it, true)
                }
            }

            value?.let {
                targetRenderer.addBlock(it, box = EMPTY_BOX.expand(0.01e-5, 0.0, 0.0))
                targetRenderer.updateAll()
            }

            resetMiningState()
            field = value
        }

    override fun enable() {
        interaction.cancelBlockBreaking()
    }

    override fun disable() {
        targetPos = null
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        val blockPos = targetPos ?: return@tickHandler
        val state = blockPos.getState()!!
        val invalid = mode.activeChoice.isInvalid(blockPos, state)
        if (invalid || blockPos.getCenterDistanceSquaredEyes() > keepRange.sq()) {
            targetPos = null
            return@tickHandler
        }

        val rotation = handleRotating(blockPos, state) ?: return@tickHandler
        handleBreaking(blockPos, state, rotation)
    }

    private fun handleRotating(blockPos: BlockPos, state: BlockState): Rotation? {
        val rotate = rotationMode.shouldRotate()

        val eyes = player.eyes
        val raytrace = raytraceBlock(
            eyes,
            blockPos,
            state,
            range = range.toDouble(),
            wallsRange = wallsRange.toDouble()
        ) ?: run {
            // don't do actions when the block is out of range
            abort(blockPos)
            return null
        }

        if (rotate) {
            RotationManager.aimAt(
                raytrace.rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotationsConfigurable,
                Priority.IMPORTANT_FOR_USAGE_2,
                ModulePacketMine
            )
        }

        shouldRotate = rotate

        return raytrace.rotation
    }

    private fun handleBreaking(blockPos: BlockPos, state: BlockState, rotation: Rotation) {
        // are we looking at the target?
        val rayTraceResult = raytraceBlock(
            max(range, wallsRange).toDouble() + 1.0,
            rotation = if (shouldRotate && (!started || rotationMode.between)) {
                RotationManager.serverRotation
            } else {
                rotation
            },
            pos = blockPos,
            state = state
        )

        if (rayTraceResult == null ||
            rayTraceResult.type != HitResult.Type.BLOCK ||
            rayTraceResult.blockPos != targetPos
        ) {
            mode.activeChoice.onCannotLookAtTarget(blockPos)
            abort(blockPos)
            return
        }

        val direction = rayTraceResult.side

        if (player.isCreative) {
            interaction.sendSequencedPacket(net.ccbluex.liquidbounce.utils.client.world) { sequence: Int ->
                interaction.breakBlock(blockPos)
                PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence)
            }
            swingMode.swing(Hand.MAIN_HAND)
            return
        }

        val slot = switchMode.getSlot(state)
        if (!started) {
            startBreaking(slot, blockPos, direction)
        } else if (mode.activeChoice.shouldUpdate(blockPos, direction, slot)) {
            updateBreakingProgress(blockPos, state, slot)
            if (progress >= 1f && !finished) {
                mode.activeChoice.finish(blockPos, direction)
            }
        }

        ModulePacketMine.direction = direction
    }

    private fun startBreaking(slot: IntObjectImmutablePair<ItemStack>?, blockPos: BlockPos, direction: Direction?) {
        switch(slot, blockPos)
        mode.activeChoice.start(blockPos, direction)
        started = true
    }

    private fun updateBreakingProgress(
        blockPos: BlockPos,
        state: BlockState,
        slot: IntObjectImmutablePair<ItemStack>?
    ) {
        progress += switchMode.getBlockBreakingDelta(blockPos, state, slot?.second())
        switch(slot, blockPos)
        val f = progress.toDouble().coerceIn(0.0..1.0) / 2
        val box = blockPos.outlineBox
        val lengthX = box.lengthX
        val lengthY = box.lengthY
        val lengthZ = box.lengthZ
        targetRenderer.updateBox(
            blockPos,
            box.expand(
                -(lengthX / 2) + lengthX * f,
                -(lengthY / 2) + lengthY * f,
                -(lengthZ / 2) + lengthZ * f
            )
        )
    }

    fun switch(slot: IntObjectImmutablePair<ItemStack>?, pos: BlockPos) {
        if (slot == null) {
            return
        }

        val shouldSwitch = switchMode.shouldSwitch()
        if (shouldSwitch && ModuleAutoTool.running) {
            ModuleAutoTool.switchToBreakBlock(pos)
        } else if (shouldSwitch) {
            SilentHotbar.selectSlotSilently(this, slot.firstInt(), 1)
        }
    }

    @Suppress("unused")
    private val mouseButtonHandler = handler<MouseButtonEvent> { event ->
        val openScreen = mc.currentScreen != null
        val unchangeableActive = !mode.activeChoice.canManuallyChange && targetPos != null
        if (openScreen || unchangeableActive || !player.abilities.allowModifyWorld) {
            return@handler
        }

        val isLeftClick = event.button == 0
        // without adding a little delay before being able to unselect / select again, selecting would be impossible
        val hasTimePassed = chronometer.hasElapsed(200)
        val hitResult = mc.crosshairTarget
        if (!isLeftClick || !hasTimePassed || hitResult == null || hitResult !is BlockHitResult) {
            return@handler
        }

        val blockPos = hitResult.blockPos
        val state = blockPos.getState()!!

        val shouldTargetBlock = mode.activeChoice.shouldTarget(blockPos, state)
        // stop when the block is clicked again
        val isCancelledByUser = blockPos.equals(targetPos)

        targetPos = if (shouldTargetBlock && world.worldBorder.contains(blockPos) && !isCancelledByUser) {
            blockPos
        } else {
            null
        }

        chronometer.reset()
    }

    @Suppress("unused")
    private val blockAttackHandler = handler<BlockAttackEvent> {
        it.cancelEvent()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        targetPos = null
    }

    @Suppress("unused")
    private val blockUpdateHandler = handler<PacketEvent> {
        if (!mode.activeChoice.stopOnStateChange) {
            return@handler
        }

        when (val packet = it.packet) {
            is BlockUpdateS2CPacket -> {
                mc.renderTaskQueue.add(Runnable { updatePosOnChange(packet.pos, packet.state) })
            }

            is ChunkDeltaUpdateS2CPacket -> {
                mc.renderTaskQueue.add(Runnable {
                    packet.visitUpdates { pos, state -> updatePosOnChange(pos, state) }
                })
            }
        }
    }

    private fun updatePosOnChange(pos: BlockPos, state: BlockState) {
        if (pos == targetPos && state.isAir) {
            targetPos = null
        }
    }

    private fun abort(pos: BlockPos, force: Boolean = false) {
        val notPossible = !started || finished || !mode.activeChoice.canAbort
        if (notPossible || !force && pos.getCenterDistanceSquaredEyes() <= keepRange.sq()) {
            return
        }

        val dir = direction ?: Direction.DOWN
        network.sendPacket(PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, dir))
        resetMiningState()
    }

    private fun resetMiningState() {
        progress = 0f
        started = false
        direction = null
        shouldRotate = rotationMode.start
        finished = false
    }

    fun setTarget(blockPos: BlockPos) {
        if (finished && mode.activeChoice.canManuallyChange || targetPos == null) {
            targetPos = blockPos
        }
    }

    @Suppress("FunctionNaming", "FunctionName")
    fun _resetTarget() {
        targetPos = null
    }

    /* tweaked minecraft code start */

    /**
     * See [BlockState.calcBlockBreakingDelta]
     */
    private fun calcBlockBreakingDelta(pos: BlockPos, state: BlockState, stack: ItemStack): Float {
        val hardness = state.getHardness(world, pos)
        if (hardness == -1f) {
            return 0f
        }

        val suitableMultiplier = if (!state.isToolRequired || stack.isSuitableFor(state)) 30 else 100
        return getBlockBreakingSpeed(state, stack) / hardness / suitableMultiplier
    }

    private fun getBlockBreakingSpeed(state: BlockState, stack: ItemStack): Float {
        var speed = stack.getMiningSpeedMultiplier(state)

        val enchantmentLevel = stack.getEnchantment(Enchantments.EFFICIENCY)
        if (speed > 1f && enchantmentLevel != 0) {
            /**
             * See: [EntityAttributes.PLAYER_MINING_EFFICIENCY]
             */
            val enchantmentAddition = enchantmentLevel.sq() + 1f
            speed += enchantmentAddition.coerceIn(0f..1024f)
        }

        if (StatusEffectUtil.hasHaste(player)) {
            speed *= 1f + (StatusEffectUtil.getHasteAmplifier(player) + 1).toFloat() * 0.2f
        }

        if (player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            val miningFatigueMultiplier = when (player.getStatusEffect(StatusEffects.MINING_FATIGUE)!!.amplifier) {
                0 -> 0.3f
                1 -> 0.09f
                2 -> 0.0027f
                3 -> 8.1E-4f
                else -> 8.1E-4f
            }

            speed *= miningFatigueMultiplier
        }

        speed *= player.getAttributeValue(EntityAttributes.PLAYER_BLOCK_BREAK_SPEED).toFloat()
        if (player.isSubmergedIn(FluidTags.WATER)) {
            speed *= player.getAttributeInstance(EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED)!!.value.toFloat()
        }

        if (!player.isOnGround) {
            speed /= 5f
        }

        return speed
    }

    /* tweaked minecraft code end */

    @Suppress("unused")
    enum class RotationMode(
        override val choiceName: String,
        val start: Boolean,
        val end: Boolean,
        val between: Boolean
    ) : NamedChoice {

        ON_START("OnStart", true, false, false),
        ON_STOP("OnStop", false, true, false),
        BOTH("Both", true, true, false),
        ALWAYS("Always", true, true, true),
        NEVER("Never", false, false, false);

        fun shouldRotate(): Boolean {
            return !started && start || end && progress >= 1f || progress < 1f && started && between
        }

    }

    enum class ToolMode(override val choiceName: String, val end: Boolean, val between: Boolean) : NamedChoice {

        ON_STOP("OnStop", true, false),
        ALWAYS("Always", true, true),
        NEVER("Never", false, false);

        fun shouldSwitch(): Boolean {
            return between || end && progress >= 1f
        }

        fun getBlockBreakingDelta(pos: BlockPos, state: BlockState, itemStack: ItemStack?): Float {
            if (!end && !between || itemStack == null) {
                return state.calcBlockBreakingDelta(player, world, pos)
            }

            return calcBlockBreakingDelta(pos, state, itemStack)
        }

        fun getSlot(state: BlockState): IntObjectImmutablePair<ItemStack>? {
            if (!end && !between) {
                return null
            }

            return ModuleAutoTool.getTool(player.inventory, state)
        }

    }

}
