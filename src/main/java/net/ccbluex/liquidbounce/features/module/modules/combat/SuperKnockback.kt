/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.base.Category
import net.ccbluex.liquidbounce.features.module.base.Module
import net.ccbluex.liquidbounce.utils.attack.CombatUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.withinChance
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.TickDelayTimer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*
import net.minecraft.client.settings.GameSettings
import kotlin.math.abs

object SuperKnockback : Module("SuperKnockback", Category.COMBAT) {

    private val chance by int("Chance", 100, 0..100, suffix = "%") { !(mode == "WTap" && wtapMode == "Dynamic") }
    private val delay by int("Delay", 0, 0..500, suffix = "ms") { !(mode == "WTap" && wtapMode == "Dynamic") }
    private val hurtTime by intRange("HurtTime", 0..10, 0..10) { !(mode == "WTap" && wtapMode == "Dynamic") }

    // TODO: Fix SprintTap flagging on prediction anti-cheats
    // TODO: Fix STap mode
    private val mode by choices(
        "Mode",
        arrayOf("WTap", "STap", "SprintTap", "Sneak", "Old", "Silent", "Packet", "SneakPacket"),
        "Old"
    )

    private val wtapMode by choices(
        "WTapMode",
        arrayOf("Normal", "Dynamic"),
        "Normal"
    ) { mode == "WTap" }

    private val ticksUntilBlock by intRange("TicksUntilBlock", 0..2, 0..5) { mode == "WTap" && wtapMode == "Normal" }
    private val reSprintTicks by intRange("ReSprintTicks", 1..2, 1..5) { mode == "WTap" && wtapMode == "Normal" }

    private val targetDistance by int("TargetDistance", 3, 1..5, suffix = "blocks") { mode == "WTap" && wtapMode == "Normal" }

    private val useDelayMultiplier by boolean("UseDelayMultiplier", true) { mode == "WTap" && wtapMode == "Normal" }

    private val sTapTicks by intRange("STapTicks", 1..2, 1..5) { mode == "STap" }

    private val sneakTicks by intRange("SneakTicks", 1..2, 1..5) { mode == "Sneak" }

    private val minEnemyRotDiffToIgnore by float("MinRotationDiffFromEnemyToIgnore", 180f, 0f..180f, suffix = "º") { !(mode == "WTap" && wtapMode == "Dynamic") }

    // TODO: Add an OnSword or OnBlocking option, in case someone is using legit AutoBlock
    private val onlyGround by boolean("OnlyGround", false) { !(mode == "WTap" && wtapMode == "Dynamic") }
    val onlyMove by boolean("OnlyMove", true) { !(mode == "WTap" && wtapMode == "Dynamic") }
    val onlyMoveForward by boolean("OnlyMoveForward", true) { onlyMove && !(mode == "WTap" && wtapMode == "Dynamic") }
    private val onlyWhenTargetGoesBack by boolean("OnlyWhenTargetGoesBack", false) { !(mode == "WTap" && wtapMode == "Dynamic") }
    private val onWeb by boolean("OnWeb", false) { !(mode == "WTap" && wtapMode == "Dynamic") }
    private val onLiquid by boolean("OnLiquid", false) { !(mode == "WTap" && wtapMode == "Dynamic") }

    private var ticks = 0
    private var forceSprintState = 0
    private val timer = MSTimer()

    // WTap
    private var blockInputTicks = ticksUntilBlock.random()
    private var blockTicksElapsed = 0
    private var startWaiting = false
    private var blockInput = false
    private var allowInputTicks = reSprintTicks.random()
    private var ticksElapsed = 0

    private val sTapTimer = TickDelayTimer(sTapTicks.first, sTapTicks.last)

    // Sneak
    private val sneakTimer = TickDelayTimer(sneakTicks.first, sneakTicks.last)

    // SprintTap2
    private var sprintTicks = 0

    override fun onToggle(state: Boolean) {
        // Make sure the user won't have their input forever blocked
        blockInput = false
        startWaiting = false
        blockTicksElapsed = 0
        ticksElapsed = 0
        sprintTicks = 0
    }

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity as? EntityLivingBase ?: return@handler

        if (mode == "WTap" && wtapMode == "Dynamic") {
            handleDynamicWTap(target)
            return@handler
        }

        val distance = player.getDistanceToEntityBox(target)

        val rotationToPlayer = toRotation(player.hitBox.center, false, target).fixedSensitivity().yaw
        val angleDifferenceToPlayer = abs(angleDifference(rotationToPlayer, target.rotationYaw))

        if (event.targetEntity.hurtTime !in hurtTime ||
            !timer.hasTimePassed(delay) ||
            onlyGround && !player.onGround ||
            (onlyMove && (!player.isMoving || onlyMoveForward && player.movementInput.moveStrafe != 0f)) ||
            !onWeb && player.isInWeb ||
            !onLiquid && player.isInLiquid ||
            !withinChance(chance)
        ) return@handler

        // Is the enemy facing their back on us?
        if (angleDifferenceToPlayer > minEnemyRotDiffToIgnore && !target.hitBox.isVecInside(player.eyes)) return@handler

        val pos = target.currPos - target.lastTickPos
        val distanceBasedOnMotion = player.getDistanceToBox(target.hitBox.offset(pos))

        // Is the entity's distance based on motion farther than the normal distance?
        if (onlyWhenTargetGoesBack && distanceBasedOnMotion >= distance) return@handler

        when (mode) {
            "Old" -> {
                // Users reported that this mode is better than the other ones
                if (player.isSprinting) {
                    sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                }

                sendPackets(
                    C0BPacketEntityAction(player, START_SPRINTING),
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SPRINTING)
                )

                player.isSprinting = true
                player.serverSprintState = true
            }

            "SprintTap", "Silent" -> if (player.isSprinting && player.serverSprintState) ticks = 2

            "Packet" -> {
                sendPackets(
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SPRINTING)
                )
            }

            "SneakPacket" -> {
                sendPackets(
                    C0BPacketEntityAction(player, STOP_SPRINTING),
                    C0BPacketEntityAction(player, START_SNEAKING),
                    C0BPacketEntityAction(player, START_SPRINTING),
                    C0BPacketEntityAction(player, STOP_SNEAKING)
                )
            }

            "WTap" -> {
                // We want the player to be sprinting before we block inputs
                if (player.isSprinting && player.serverSprintState && !blockInput && !startWaiting) {
                    val delayMultiplier = if (useDelayMultiplier) 1.0 / (abs(targetDistance - distance) + 1.0) else 1.0

                    blockInputTicks = (ticksUntilBlock.random().toDouble() * delayMultiplier).toInt()
                    blockInput = blockInputTicks == 0
    
                    if (!blockInput) {
                        startWaiting = true
                    }

                    allowInputTicks = (reSprintTicks.random().toDouble() * delayMultiplier).toInt()
                }
            }

            "STap" -> {
                if (player.isSprinting && player.serverSprintState) {
                    mc.gameSettings.keyBindForward.pressed = false
                    mc.gameSettings.keyBindBack.pressed = true
                }
            }

            "Sneak" -> {
                if (player.isSprinting && player.serverSprintState && !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && !mc.gameSettings.keyBindSneak.pressed) {
                    mc.gameSettings.keyBindSneak.pressed = true
                }
            }
        }

        timer.reset()
    }

    private fun handleDynamicWTap(target: EntityLivingBase) {
        val player = mc.thePlayer ?: return
        val distance = player.getDistanceToEntityBox(target)

        val rotationToPlayer = toRotation(player.hitBox.center, false, target).fixedSensitivity().yaw
        val angleDifferenceToPlayer = abs(angleDifference(rotationToPlayer, target.rotationYaw))

        val pos = target.currPos - target.lastTickPos
        val distanceBasedOnMotion = player.getDistanceToBox(target.hitBox.offset(pos))
        val distanceDifference = distance + distanceBasedOnMotion
        
        if (angleDifferenceToPlayer > 80f || CombatUtils.lastValidAttack.hasTimePassed(10)) return

        // We want the player to be sprinting before we block inputs
        if (player.isSprinting && player.serverSprintState && !blockInput && !startWaiting) {
            val multiplier = if (CombatUtils.combo > 1) if (distanceDifference > 0f) 1.5f / distanceDifference + 0.8f else 1.8f * -distanceDifference + 1f else 1f

            blockInputTicks = (1f * multiplier).toInt()
            blockInput = blockInputTicks == 0
    
            if (!blockInput) {
                startWaiting = true
            }

            allowInputTicks = (1f * multiplier).toInt()
        }
    }

    val onPostSprintUpdate = handler<PostSprintUpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (mode != "SprintTap") return@handler

        when (ticks) {
            2 -> {
                player.isSprinting = false
                forceSprintState = 2
                ticks--
            }

            1 -> {
                if (player.movementInput.moveForward > 0.8)
                    player.isSprinting = true

                forceSprintState = 1
                ticks--
            }

            else -> {
                forceSprintState = 0
            }
        }
    }

    val onUpdate = handler<UpdateEvent> {
        when (mode) {
            "WTap" -> {
                if (blockInput && ticksElapsed++ >= allowInputTicks) {
                    blockInput = false
                    ticksElapsed = 0
                } else {
                    if (startWaiting) {
                        blockInput = blockTicksElapsed++ >= blockInputTicks

                        if (blockInput) {
                            startWaiting = false
                            blockTicksElapsed = 0
                        }
                    }
                }
            }

            "STap" -> {
                if (mc.gameSettings.keyBindBack.pressed && !GameSettings.isKeyDown(mc.gameSettings.keyBindBack) &&
                    sTapTimer.resetIfPassed()
                )
                    mc.gameSettings.keyBindBack.pressed = false
                    mc.gameSettings.keyBindForward.pressed = true
            }

            "Sneak" -> {
                if (mc.gameSettings.keyBindSneak.pressed && !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && sneakTimer.resetIfPassed())
                    mc.gameSettings.keyBindSneak.pressed = false
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (mode == "Silent" && event.packet is C03PacketPlayer) {
            if (ticks == 2) {
                sendPacket(C0BPacketEntityAction(player, STOP_SPRINTING))
                ticks--
            } else if (ticks == 1 && player.isSprinting) {
                sendPacket(C0BPacketEntityAction(player, START_SPRINTING))
                ticks--
            }
        }
    }

    fun shouldBlockInput() = handleEvents() && mode == "WTap" && blockInput
    fun breakSprint() = handleEvents() && forceSprintState == 2 && mode == "SprintTap"
    fun startSprint() = handleEvents() && forceSprintState == 1 && mode == "SprintTap"

    override val tag
        get() = mode
}
