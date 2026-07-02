/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.legit.*
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.packet.*
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.withinChance
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*
import kotlin.math.abs

object SuperKnockback : Module("SuperKnockback", Category.COMBAT) {

    private val chance by int("Chance", 100, 0..100, suffix = "%")
    private val delay by int("Delay", 0, 0..1000, suffix = "ms")
    private val hurtTime by intRange("HurtTime", 0..10, 0..10)

    private val superKnockbackModes = arrayOf(
        // Legit
        WTap, STap, SprintTap, Sneak,

        // Packet
        Old, Silent, Packet, SneakPacket
    )

    private val modes = superKnockbackModes.map { it.modeName }.toTypedArray()

    val mode by choices("Mode", modes, "SprintTap")

    val ticksUntilBlock by intRange("TicksUntilBlock", 0..2, 0..5) { mode == "WTap" }
    val reSprintTicks by intRange("ReSprintTicks", 1..2, 1..5) { mode == "WTap" }
    val useDelayMultiplier by boolean("UseDelayMultiplier", true) { mode == "WTap" }
    val targetDistance by int("TargetDistance", 3, 1..5, suffix = "blocks") { mode == "WTap" && useDelayMultiplier }

    val sTapTicks by intRange("STapTicks", 1..2, 1..5) { mode == "STap" }

    val sneakTicks by intRange("SneakTicks", 1..2, 1..5) { mode == "Sneak" }

    private val minEnemyRotDiffToIgnore by float("MinRotationDiffFromEnemyToIgnore", 180f, 0f..180f, suffix = "º")

    // TODO: Add an OnSword or OnBlocking option, in case someone is using legit AutoBlock
    private val onlyGround by boolean("OnlyGround", false)
    val onlyMove by boolean("OnlyMove", true)
    val onlyMoveForward by boolean("OnlyMoveForward", true) { onlyMove }
    private val onlyWhenTargetGoesBack by boolean("OnlyWhenTargetGoesBack", false)
    private val onWeb by boolean("OnWeb", false)
    private val onLiquid by boolean("OnLiquid", false)

    private val timer = MSTimer()
    var ticks = 0

    override fun onToggle(state: Boolean) {
        modeModule.onToggle(state)
    }

    val onAttack = handler<AttackEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val target = event.targetEntity as? EntityLivingBase ?: return@handler
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

        modeModule.onAttack(event)

        timer.reset()
    }

    val onUpdate = handler<UpdateEvent> { event ->
        modeModule.onUpdate(event)
    }

    val onPostSprintUpdate = handler<PostSprintUpdateEvent> { event ->
        modeModule.onPostSprintUpdate(event)
    }

    override val tag
        get() = mode

    private val modeModule
        get() = superKnockbackModes.find { it.modeName == mode }!!

    fun breakSprint() = handleEvents() && mode == "SprintTap" && SprintTap.forceSprintState == 2
    fun startSprint() = handleEvents() && mode == "SprintTap" && SprintTap.forceSprintState == 1

    fun shouldBlockInput() = handleEvents() && mode == "WTap" && WTap.blockInput
}