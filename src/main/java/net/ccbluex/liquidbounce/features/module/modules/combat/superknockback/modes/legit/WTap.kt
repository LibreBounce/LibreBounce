/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.legit

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.useDelayMultiplier
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.targetDistance
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.ticksUntilBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.SuperKnockback.reSprintTicks
import net.ccbluex.liquidbounce.features.module.modules.combat.superknockback.modes.SuperKnockbackMode
import net.minecraft.entity.EntityLivingBase
import kotlin.math.abs

object WTap : SuperKnockbackMode("WTap") {

    private var blockInputTicks = ticksUntilBlock.random() ?: 3
    private var allowInputTicks = reSprintTicks.random() ?: 3
    private var blockTicksElapsed = 0
    private var ticksElapsed = 0
    private var startWaiting = false
    var blockInput = false

    override fun onToggle(state: Boolean) {
        // Make sure the user won't have their input forever blocked
        blockInput = false
        startWaiting = false
        blockTicksElapsed = 0
        ticksElapsed = 0
    }

    override fun onAttack(event: AttackEvent) {
        val player = mc.thePlayer ?: return
        val target = event.targetEntity as? EntityLivingBase ?: return
        val distance = player.getDistanceToEntityBox(target)

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

    override fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer

        if (blockInput) {
            if (ticksElapsed++ >= allowInputTicks) {
                blockInput = false
                ticksElapsed = 0
            }
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
}