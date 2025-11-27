/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.entity.Entity

object CombatJump : Module("CombatJump", Category.COMBAT) {

    private val allowedJumpDistance by floatRange("AllowedJumpDistance", 5f..8f, 0f..12f)
    private val endDistance by floatRange("EndDistance", 3.05f..3.25f, 0f..6f)
    private val onlyMove by boolean("OnlyMove", true)

    private val predictClientMovement by int("PredictClientMovement", 6, 0..10, suffix = "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..2f)

    private val debug by boolean("Debug", false).subjective()

    // Anti-cheats such as Grim flag when you don't jump on this event
    val onStrafe = handler<StrafeEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        // TODO: Use a different target picking system
        val target = KillAura.target ?: return@handler

        if (onlyMove && !player.isMoving) return@handler

        if (player.getDistanceToEntityBox(target) !in allowedJumpDistance) return@handler

        if (player.onGround && shouldJump(target)) {
            player.tryJump()

            if (debug) chat("(CombatJump) Jumped to the target")
        }
    }

    private fun shouldJump(target: Entity): Boolean {
        val player = mc.thePlayer ?: return false
        val modifiedInput = RotationUtils.modifiedInput
        val simPlayer = SimulatedPlayer.fromClientPlayer(modifiedInput)
    
        val prediction = target.currPos.subtract(target.prevPos).times(predictEnemyPosition.toDouble())
        val boundingBox = target.hitBox.offset(prediction)

        val distance = player.getDistanceToEntityBox(target)

        val (currPos, prevPos) = player.currPos to player.prevPos

        repeat(1) {
            if (simPlayer.onGround) {
                modifiedInput.jump = true

                if (debug) chat("(CombatJump) Simulated a jump")
            }
        }

        repeat(predictClientMovement - 1) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val simDist = player.getDistanceToBox(boundingBox)

        //val fallingPlayer = FallingPlayer(player)

        player.setPosAndPrevPos(currPos, prevPos)

        // TODO: Use fallingPlayer for ground hit checking
        return simDist in endDistance && simDist < distance && !simPlayer.onGround
    }
}