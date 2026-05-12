/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.runWithSimulatedPosition
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.Fucker
import net.ccbluex.liquidbounce.features.module.modules.world.Nuker
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.*
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.resetLastAttackedTicks
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.potion.Potion
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.roundToInt

object Aimbot : Module("Aimbot", Category.COMBAT) {
    
    // Range
    private val range by floatRange("Range", 0f..3f, 1f..8f, suffix = "blocks")
    private val throughWallsRange by floatRange("ThroughWallsRange", 0..3f, 0f..8f, suffix = "blocks")

    private val activationSlot by boolean("ActivationSlot", false)
    private val preferredSlot by int("PreferredSlot", 1, 1..9) { activationSlot }

    private val clickOnly by boolean("ClickOnly", false)
    private val notOnConsume by boolean("NotOnConsume", true)
    
    // Modes
    private val priority by choices(
        "Priority", arrayOf(
            "Optimal",
            "Health",
            "Distance",
            "Direction",
            "LivingTime",
            "Armor",
            "HurtResistance",
            "HurtTime",
            "HealthAbsorption",
            "RegenAmplifier",
            "OnLadder",
            "InLiquid",
            "InWeb"
        ), "Distance"
    )
    private val targetMode by choices("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val limitedMultiTargets by int("LimitedMultiTargets", 0, 0..50) { targetMode == "Multi" }

    private val maxSwitchFOV by float("MaxSwitchFOV", 90f, 30f..180f, suffix = "º") { targetMode == "Switch" }
    private val switchDelay by int("SwitchDelay", 15, 1..1000, suffix = "ms") { targetMode == "Switch" }

    private val autoF5 by boolean("AutoF5", false).subjective()
    private val onScaffold by boolean("OnScaffold", false)
    private val onDestroyBlock by boolean("OnDestroyBlock", false)

    // Rotations
    private val options = RotationSettings(this).withoutKeepRotation()

    private val generateSpotBasedOnDistance by boolean("GenerateSpotBasedOnDistance", false) { options.rotationsActive }

    private val randomization = RandomizationSettings(this) { options.rotationsActive }
    private val outBorder by boolean("Outborder", false) { options.rotationsActive }

    private val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
        coercedPoint.displayName
    }
    private val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
        coercedPoint.displayName
    }

    private val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    private val horizontalBodySearchRange by floatRange(
        "HorizontalBodySearchRange", 0f..1f, 0f..1f
    ) { options.rotationsActive }

    private val fov by float("FOV", 180f, 0f..180f, suffix = "º")

    // Prediction
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5, suffix = "ticks")
    private val predictOnlyWhenOutOfRange by boolean(
        "PredictOnlyWhenOutOfRange", false
    ) { predictClientMovement != 0 }
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f)

    // Visuals
    private val renderAimPointBox by boolean("RenderAimPointBox", false).subjective()
    private val aimPointBoxColor by color("AimPointBoxColor", Color.CYAN) { renderAimPointBox }.subjective()
    private val aimPointBoxSize by float("AimPointBoxSize", 0.1f, 0f..0.2F) { renderAimPointBox }.subjective()

    private val mark by choices("Mark", arrayOf("None", "Platform", "Box", "Circle"), "Circle").subjective()

    private val markColor by color("MarkColor", Color(255, 0, 0, 70)) { mark in arrayOf("Platform", "Box") }.subjective()
    private val markHittableColor by color("MarkHittableColor", Color(37, 126, 255, 70)) { mark in arrayOf("Platform", "Box") }.subjective()

    // Circle options
    private val circleStartColor by color("CircleStartColor", Color.BLUE) { mark == "Circle" }.subjective()
    private val circleEndColor by color("CircleEndColor", Color.CYAN.withAlpha(0)) { mark == "Circle" }.subjective()
    private val fillInnerCircle by boolean("FillInnerCircle", false) { mark == "Circle" }.subjective()
    private val withHeight by boolean("WithHeight", true) { mark == "Circle" }.subjective()
    private val animateHeight by boolean("AnimateHeight", false) { withHeight }.subjective()
    private val heightRange by floatRange("HeightRange", 0.0f..0.4f, -2f..2f) { withHeight }.subjective()
    private val extraWidth by float("ExtraWidth", 0F, 0F..2F) { mark == "Circle" }.subjective()
    private val animateCircleY by boolean("AnimateCircleY", true) { fillInnerCircle || withHeight }.subjective()
    private val circleYRange by floatRange("CircleYRange", 0F..0.5F, 0F..2F) { animateCircleY }.subjective()
    private val duration by float(
        "Duration", 1.5F, 0.5F..3F, suffix = "Seconds"
    ) { animateCircleY || animateHeight }.subjective()

    // Box option
    private val boxOutline by boolean("Outline", true) { mark == "Box" }.subjective()

    // Target
    var target: EntityLivingBase? = null
    private val prevTargetEntities = mutableListOf<Int>()

    // Container Delay
    private var containerOpen = -1L

    // Switch Delay
    private val switchTimer = MSTimer()

    // Swing fails
    private val swingFails = mutableListOf<SwingFailData>()

    override fun onToggle(state: Boolean) {
        target = null
        prevTargetEntities.clear()
        clicks = 0

        if (autoF5) mc.gameSettings.thirdPersonView = 0

        synchronized(swingFails) {
            swingFails.clear()
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        update()
    }

    fun update() {
        if (cancelRun) return

        // Update target
        updateTarget()

        if (autoF5) {
            if (mc.gameSettings.thirdPersonView != 1 && target != null) {
                mc.gameSettings.thirdPersonView = 1
            }
        }
    }

    /**
     * Tick event
     */
    val onTick = handler<GameTickEvent>(priority = 2) {
        val player = mc.thePlayer ?: return@handler

        if (shouldPrioritize()) {
            target = null
            return@handler
        }

        if (clickOnly && !mc.gameSettings.keyBindAttack.isKeyDown && !AutoClicker.handleEvents()) {
            clicks = 0
            return@handler
        }

        if (cancelRun) {
            target = null
            return@handler
        }
    }

    /**
     * Render event
     */
    val onRender3D = handler<Render3DEvent> {
        handleFailedSwings()

        drawAimPointBox()

        if (cancelRun) {
            target = null
            return@handler
        }

        target ?: return@handler

        val hittableColor = if (target.hurtTime == 0) markHittableColor else markColor

        if (targetMode != "Multi") {
            when (mark) {
                "None" -> return@handler
                "Platform" -> drawPlatform(target!!, hittableColor)
                "Box" -> drawEntityBox(target!!, hittableColor, boxOutline)
                "Circle" -> drawCircle(
                    target!!,
                    duration * 1000F,
                    heightRange.takeIf { animateHeight } ?: heightRange.endInclusive..heightRange.endInclusive,
                    extraWidth,
                    fillInnerCircle,
                    withHeight,
                    circleYRange.takeIf { animateCircleY },
                    circleStartColor.rgb,
                    circleEndColor.rgb
                )
            }
        }
    }

    /**
     * Attack event
     */
    val attackEvent = handler<AttackEvent> {
        // TODO: Use target, instead
        val currentTarget = this.target ?: return

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (!isLastClick) return

        val switchMode = targetMode == "Switch"

        if (!switchMode || switchTimer.hasTimePassed(switchDelay)) {
            prevTargetEntities += currentTarget.entityId

            if (switchMode) {
                switchTimer.reset()
            }
        }

        if (shouldPrioritize()) return

        // Randomizes scan range after hit
        randomizedScanRange = scanRange.random()

        resetLastAttackedTicks()
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        if (shouldPrioritize()) return

        // Reset fixed target to null
        target = null

        val switchMode = targetMode == "Switch"

        val world = mc.theWorld ?: return
        val player = mc.thePlayer ?: return

        var bestTarget: EntityLivingBase? = null
        var bestValue: Double? = null

        for (entity in world.loadedEntityList) {
            if (entity !is EntityLivingBase || !isSelected(
                    entity, true
                ) || switchMode && entity.entityId in prevTargetEntities
            ) continue

            val distance = Backtrack.runWithNearestTrackedDistance(entity) { player.getDistanceToEntityBox(entity) }

            if (switchMode && distance !in range && prevTargetEntities.isNotEmpty()) continue

            val entityFov = rotationDifference(entity)

            if (distance !in maxRange || fov != 180F && entityFov > fov) continue

            if (switchMode && !isLookingOnEntities(entity, maxSwitchFOV.toDouble())) continue

            // Credits to Gugustus / Augustus b2.6
            // TODO: Maybe we should also prioritize players that are looking at you, with weapons (or without), and breaking blocks (could be possibly trying to break your bed?)
            val optimal = (distance * 2.0) + (entity.health.toDouble() + entity.absorptionAmount) + (entity.hurtTime.toDouble() * 4.0) + (entity.totalArmorValue.toDouble() / 2.0) + (entityFov.toDouble() / 2.0)

            val currentValue = when (priority) {
                "Optimal" -> optimal
                "Distance" -> distance
                "Direction" -> entityFov.toDouble()
                "Health" -> entity.health.toDouble()
                "LivingTime" -> -entity.ticksExisted.toDouble()
                "Armor" -> entity.totalArmorValue.toDouble()
                "HurtResistance" -> entity.hurtResistantTime.toDouble()
                "HurtTime" -> entity.hurtTime.toDouble()
                "HealthAbsorption" -> (entity.health + entity.absorptionAmount).toDouble()
                "RegenAmplifier" -> if (entity.isPotionActive(Potion.regeneration)) {
                    entity.getActivePotionEffect(Potion.regeneration).amplifier.toDouble()
                } else -1.0

                "InWeb" -> if (entity.isInWeb) -1.0 else Double.MAX_VALUE
                "OnLadder" -> if (entity.isOnLadder) -1.0 else Double.MAX_VALUE
                "InLiquid" -> if (entity.isInWater || entity.isInLava) -1.0 else Double.MAX_VALUE
                else -> null
            } ?: continue

            if (bestValue == null || currentValue < bestValue) {
                bestValue = currentValue
                bestTarget = entity
            }
        }

        if (bestTarget != null) {
            if (Backtrack.runWithNearestTrackedDistance(bestTarget) { updateRotations(bestTarget) }) {
                target = bestTarget
                return
            }
        }

        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Update rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        if (shouldPrioritize()) return false

        val prediction = entity.currPos.subtract(entity.prevPos).times(2 + predictEnemyPosition.toDouble())
        val boundingBox = entity.hitBox.offset(prediction)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        simPlayer.rotationYaw = (currentRotation ?: player.rotation).yaw

        var pos = currPos

        repeat(predictClientMovement) {
            val previousPos = simPlayer.pos

            simPlayer.tick()

            player.setPosAndPrevPos(simPlayer.pos)

            val simDist = player.getDistanceToEntityBox(entity)

            player.setPosAndPrevPos(previousPos)

            val prevDist = player.getDistanceToEntityBox(entity)

            player.setPosAndPrevPos(currPos, oldPos)
            pos = simPlayer.pos

            if (predictOnlyWhenOutOfRange && simDist <= range && simDist <= prevDist) {
                return@repeat
            }

            pos = previousPos
        }

        player.setPosAndPrevPos(pos)

        val rotation = searchCenter(
            boundingBox,
            generateSpotBasedOnDistance,
            outBorder,
            randomization,
            predict = false,
            lookRange = range + randomizedScanRange,
            attackRange = range,
            throughWallsRange = throughWallsRange,
            bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
            horizontalSearch = horizontalBodySearchRange
        )

        if (rotation != null) setTargetRotation(rotation, options = options)

        player.setPosAndPrevPos(currPos, oldPos)

        return rotation != null
    }

    private fun checkIfAimingAtBox(
        targetToCheck: Entity, currentRotation: Rotation, eyes: Vec3, onSuccess: () -> Unit,
        onFail: () -> Unit = { },
    ) {
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            onSuccess()
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        if (intercept != null) {
            // Is the entity box raycast vector visible? If not, check through-wall range
            if (isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange) {
                onSuccess()
                return
            }
        }

        onFail()
    }

    private fun switchToSlot(slot: Int) {
        SilentHotbar.selectSlotSilently(this, slot, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }

    private fun shouldPrioritize(): Boolean = when {
        !onScaffold && (Scaffold.handleEvents() && (Scaffold.placeRotation != null || currentRotation != null) || Tower.handleEvents() && Tower.isTowering) -> true

        !onDestroyBlock && (Fucker.handleEvents() && !Fucker.noHit && Fucker.pos != null && !Fucker.isOwnBed || Nuker.handleEvents()) -> true

        activationSlot && SilentHotbar.currentSlot != preferredSlot - 1 -> true

        else -> false
    }

    private fun handleFailedSwings() {
        if (!renderBoxOnSwingFail) return

        val box = AxisAlignedBB(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        synchronized(swingFails) {
            val fadeSeconds = renderBoxFadeSeconds * 1000L
            val colorSettings = renderBoxColor

            val renderManager = mc.renderManager

            swingFails.removeAll {
                val timestamp = System.currentTimeMillis() - it.startTime
                val transparency = (0f..255f).lerpWith(1 - (timestamp / fadeSeconds).coerceAtMost(1.0F))

                val offsetBox = box.offset(it.vec3 - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offsetBox, colorSettings.color(a = transparency.roundToInt()))

                timestamp > fadeSeconds
            }
        }
    }

    private fun drawAimPointBox() {
        val player = mc.thePlayer ?: return
        val target = this.target ?: return

        if (!renderAimPointBox) {
            return
        }

        val f = aimPointBoxSize.toDouble()

        val box = AxisAlignedBB(0.0, 0.0, 0.0, f, f, f)

        val renderManager = mc.renderManager

        runWithSimulatedPosition(player, player.interpolatedPosition(player.prevPos)) {
            runWithSimulatedPosition(target, target.interpolatedPosition(target.prevPos)) {
                val rotationVec = player.eyes + getVectorForRotation(
                    serverRotation.lerpWith(currentRotation ?: player.rotation, mc.timer.renderPartialTicks)
                ) * player.getDistanceToEntityBox(target).coerceAtMost(range.toDouble())

                val offSetBox = box.offset(rotationVec - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offSetBox, aimPointBoxColor)
            }
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun
        inline get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer) || notOnConsume && isConsumingItem()

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    /**
     * Range
     */
    private val maxRange
        get() = max(range, throughWallsRange)

    private fun getRange(entity: Entity) =
        if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRange) range else throughWallsRange

    /**
     * HUD Tag
     */
    override val tag
        get() = targetMode

    val isBlockingChestAura
        get() = handleEvents() && target != null
}

data class SwingFailData(val vec3: Vec3, val startTime: Long)
