package net.ccbluex.liquidbounce.utils.attack

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.PacketEvent
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
import net.minecraft.potion.Potion.blindness
import net.minecraft.network.play.server.S19PacketEntityStatus
import kotlin.math.abs

object CombatUtils : MinecraftInstance, Listenable {
    var lastValidAttack = MSTimer()
    var lastValidAttackIsCrit = false
    var lastAttackCrit = false
    var lastAttackBlocked = false
    var lastTarget: EntityLivingBase? = null
    var theoreticalHitDelay = MSTimer()
    var predictedHurtDelay = 0
    var combo = 0

    val onAttack = handler<AttackEvent>(priority = 4) { event ->
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
            val targetNull = lastTarget == null

            lastTarget = null
            lastAttackCrit = false
            lastAttackBlocked = false
            combo = 0

            val seconds = if (resetTargetAfter == 1) "second" else "seconds"

            if (debug && targetNull) chat("Reset due to $resetTargetAfter $seconds passing")
        }
    }

    // Credits to EvergreenHUD for this code!
    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is S19PacketEntityStatus) {
            val packet = event.packet as S19PacketEntityStatus

            if (packet.opCode.toInt() != 2)
                return@handler

            val target = packet.getEntity(mc.theWorld) ?: return@handler

            if (target.entityId == lastTarget?.entityId) {
                combo++

                if (debug) chat("Theoretical hit delay: ${theoreticalHitDelay.getTime()}")

                predictedHurtDelay = theoreticalHitDelay.getTime()

                theoreticalHitDelay.reset()
            } else if (target.entityId == mc.thePlayer.entityId) {
                combo = 0
            }
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