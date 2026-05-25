/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.max

object Test : Module("Test", Category.FUN, subjective = true) {

    private val potentialDelayDistance by floatRange("PotentialDelayDistance", 5f..8f, 0f..16f)
    private val legitDistance by floatRange("LegitDistance", 3.0f..3.5f, 0f..6f)

    private val differenceToFlag by int("DifferenceToFlag", 30, 0..1000, suffix = "ms")
    private val checkFakeLagging by boolean("CheckFakeLagging", true)

    var target: Entity? = null

    //val timer = MSTimer()
    var targetOutOfRangePing = 0
    var targetInRangePing = 0
    var lowestTargetPing = 0
    var potentiallyCheating = false

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity ?: return@handler
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val fixedTarget = (KillAura.target as Entity) ?: target ?: return@handler

        if (checkFakeLagging) checkFakeLagging(fixedTarget)
    }

    private fun checkFakeLagging(target: Entity) {
        val player = mc.thePlayer

        val targetPing = (target as EntityPlayer).getPing()

        lowestTargetPing = if (targetPing < lowestTargetPing) targetPing else lowestTargetPing

        if (player.getDistanceToEntityBox(target) in potentialDelayDistance)
            targetOutOfRangePing = targetPing

        if (player.getDistanceToEntityBox(target) in legitDistance)
            targetInRangePing = targetPing

        if (max(targetOutOfRangePing, targetInRangePing) > lowestTargetPing + differenceToFlag)
            potentiallyCheating = true
        else potentiallyCheating = false

        chat("(Test) Potentially fake lagging: $potentiallyCheating (real ping: ${targetInRangePing}, potential delay: ${targetOutOfRangePing}, difference to flag: ${differenceToFlag})")
    }
}