package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import com.google.common.collect.Lists
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.minecraft.network.packet.s2c.play.*

object KillAuraVelocityHit : ToggleableConfigurable(ModuleKillAura, "VelocityHit", false) {

    val extendRange by float("ExtendRange", 1.0f, 0.1f..2.0f)
    private val whenLag by boolean("OnlyWhileLagging", false)

    private var considerVelocityHit = false
    private var damageReceived = false
    private var onGroundTicks = 0
    private var isPossible = false

    private var timer = Chronometer()
    private var lastPacketTime = Lists.newLinkedList<Long>()

    private const val SAMPLE_SIZE = 10

    val isVelocityHitPossible
        get() = super.running && isPossible

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent>(priority = 1) { event ->
        val packet = event.packet

        if (event.origin == TransferOrigin.RECEIVE) {
            addRecentPacketTime()
        }

        when {
            packet is EntityDamageS2CPacket && packet.entityId == player.id -> {
                damageReceived = true
            }
            packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && damageReceived -> {
                considerVelocityHit = true
            }
        }
    }

    @Suppress("unused")
    private val gameHandler = tickHandler {
        if (player.isDead || player.isSpectator) {
            return@tickHandler
        }

        val enemy = ModuleKillAura.targetTracker.lockedOnTarget
        var lagging = isLagging() || PacketQueueManager.isLagging

        if (!whenLag) {
            lagging = true
        }

        if (enemy == null) {
            reset()
            return@tickHandler
        }

        val isInExtendedRange = player.distanceTo(enemy) <= ModuleKillAura.extendedReach
        isPossible = lagging && considerVelocityHit && isInExtendedRange

        if ((player.isOnGround && isPossible) || (player.fallDistance > 0.3 && isPossible)) {
            onGroundTicks++
        }

        if (onGroundTicks > 5) {
            reset()
        }
    }

    fun reset() {
        isPossible = false
        considerVelocityHit = false
        damageReceived = false
        onGroundTicks = 0
    }

    private fun addRecentPacketTime() {
        lastPacketTime.add(timer.elapsed)
        timer.reset()

        if (lastPacketTime.size > SAMPLE_SIZE) {
            for (i in 0..<SAMPLE_SIZE) {
                lastPacketTime.removeAt(0)
            }
        }
    }

    private fun isLagging(): Boolean {
        if (lastPacketTime.size != SAMPLE_SIZE) {
            return false
        }

        var sumTime: Long = 0
        for (i in 0..<SAMPLE_SIZE) {
            sumTime += lastPacketTime[i]
        }

        val diff: Double = sumTime / SAMPLE_SIZE.toDouble();
        return diff > 0.5
    }

}
