/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
/*package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase

object CombatJump : Module("CombatJump", Category.COMBAT) {

    private val endDistance by floatRange("EndDistance", 3.05f..3.25f, 0f..8f)
    private val onlyMove by boolean("OnlyMove", true)

    private val predictClientMovement by boolean("PredictClientMovement", 6, 0..10, suffix = "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..2f)

    private val debug by boolean("Debug", false).subjective()

    // Anti-cheats such as Grim flag when you don't do it on this event
    val onStrafe = handler<StrafeEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val target = KillAura.target ?: return@handler

        if (onlyMove && !player.isMoving) return@handler

        if (shouldJump(target)) {
            player.tryJump()

            if (debug) chat("(CombatJump) Jumped to the target")
        }
    }

    private fun shouldJump(): Boolean {
        val distance = player.getDistanceToEntityBox(target)

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)
    
        val simDist = player.getDistanceToEntityBox(target)

        val prediction = target.currPos.subtract(target.prevPos).times(predictEnemyPosition.toDouble())
        val boundingBox = target.hitBox.offset(prediction)

        val currPos = player.currPos
        val prevPos = player.prevPos

        if (simPlayer.onGround) simPlayer.jump()

        repeat(predictClientMovement) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        simDist = player.getDistanceToBox(boundingBox)

        player.setPosAndPrevPos(currPos, prevPos)

        return simDist in distance
        
    }
}*/