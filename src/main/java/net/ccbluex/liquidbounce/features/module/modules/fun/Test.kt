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
import net.minecraft.entity.EntityPlayer
import net.minecraft.entity.EntityLivingBase

object Test : Module("Test", Category.FUN, subjective = true) {

    private val potentialDelayDistance by floatRange("PotentialDelayDistance", 5f..8f, 0f..16f)
    private val legitDistance by floatRange("LegitDistance", 3.0f..3.5f, 0f..6f)

    private val differenceToFlag by int("DifferenceToFlag", 30, 0..1000, suffix = "ms")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..10f)

    var target: Entity? = null

    val timer = MSTimer()
    var targetPotentialDelay = 0
    var targetRealPing = 0

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity ?: return@handler
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val fixedTarget = (KillAura.target as Entity) ?: target ?: return@handler

        if (player.getDistanceToEntityBox(fixedTarget) >= potentialDelayDistance) targetPotentialDelay = (fixedTarget as EntityPlayer).getPing()

        if (isFakeLagging(fixedTarget)) chat("(Test) Target may be using fake lag (real ping: ${targetRealPing}, potential delay: ${targetPotentialDelay}, difference to flag: ${differenceToFlag})")
    }

    private fun isFakeLagging(target: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        if (player.getDistanceToEntityBox(fixedTarget) <= legitDistance) targetRealPing = (target as EntityPlayer).getPing()

        if (targetRealPing < targetPotentialDelay - differenceToFlag)
            return true
        else chat("(Test) Target is probably legit (real ping: ${targetRealPing}, potential delay: ${targetPotentialDelay}, difference to flag: ${differenceToFlag})")

        return false
    }
}