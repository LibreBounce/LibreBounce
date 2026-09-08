package net.ccbluex.liquidbounce.utils.attack

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.HitDetector.debug
import net.ccbluex.liquidbounce.features.module.modules.combat.HitDetector.hitDelay
import net.ccbluex.liquidbounce.features.module.modules.combat.HitDetector.resetTargetAfter
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import kotlin.math.abs

object CombatUtils : MinecraftInstance, Listenable {
    var lastValidAttack = MSTimer()
    var lastValidAttackIsCrit = false
    var lastAttackCrit = false
    var lastAttackBlocked = false
    var lastTarget: EntityLivingBase? = null

    val onAttack = handler<AttackEvent> { event ->
        if (lastTarget != event.targetEntity) {
            lastTarget = event.targetEntity!! as EntityLivingBase?
            lastValidAttack.reset()
            
            if (debug) chat("Reset target stats due to target changing!")
        }

        if (lastValidAttack.hasTimePassed(hitDelay)) {
            lastValidAttack.reset()
            lastValidAttackIsCrit = canCritHit(mc.thePlayer)
        }

        lastAttackCrit = canCritHit(mc.thePlayer)
        lastAttackBlocked = (event.targetEntity!! as EntityPlayer).isBlocking

        if (debug) chat("Hit delay: $hitDelay, last valid attack: ${abs(lastValidAttack.getTime())}, is the last attack a critical hit: $lastAttackCrit")
    }

    val onUpdate = handler<UpdateEvent> { event ->
        if (lastValidAttack.hasTimePassed(resetTargetAfter * 1000)) {
            lastTarget = null
            lastAttackCrit = false
            lastAttackBlocked = false

            val seconds = if (resetTargetAfter == 1) "second" else "seconds"
            if (debug) chat("Reset due to $resetTargetAfter $seconds passing")
        }
    }

    val timeUntilHit = (hitDelay - lastValidAttack.getTime()).coerceAtLeast(0)

    fun canHit(): Boolean = lastValidAttack.hasTimePassed(hitDelay)
    fun canHit(customHurtTime: Int) = customHurtTime <= hitDelay / 50

    fun canCritHit(player: EntityPlayer): Boolean =
        player.fallDistance > 0 &&
        !player.isOnLadder &&
        !player.isInWater &&
        !player.isPotionActive(blindness) &&
        player.ridingEntity == null
}