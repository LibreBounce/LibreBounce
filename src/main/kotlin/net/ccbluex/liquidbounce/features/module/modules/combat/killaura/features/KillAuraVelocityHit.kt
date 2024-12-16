package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.ModulePacketLogger
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.*

object KillAuraVelocityHit : ToggleableConfigurable(ModuleKillAura, "VelocityHit", false) {

    private var considerVelocityHit = false
    private var damageReceived = false
    private var onGroundTicks = 0
    private var isPossible = false

    val extensionRange by float("Extension", 1.0f, 0.1f..2.0f)
    val onLag by boolean("OnlyWhileLagging", false)

    private var timer: Chronometer = Chronometer()
    private var lastPacketTime: ArrayList<Long> = ArrayList()

    const val sampleSize = 10

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent>(priority = 1) { event ->

        val packet = event.packet

        if (event.origin == TransferOrigin.RECEIVE) {
            addRecentPacketTime()
        }

        if (packet is EntityDamageS2CPacket && packet.entityId == player.id) {
            damageReceived = true
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id && damageReceived) {
            considerVelocityHit = true
        }
    }

    @Suppress("unused")
    private val gameHandler = tickHandler {

        if (player.isDead || player.isSpectator) {
            return@tickHandler
        }

        var enemy: LivingEntity? = ModuleKillAura.targetTracker.lockedOnTarget
        var lagging: Boolean = isLagging() || PacketQueueManager.isLagging

        if (!onLag) {
            lagging = true
}

        var isTracking: Boolean = enemy != null

        if (enemy == null) {
            reset()
            return@tickHandler
        }

        var distanceToEnemy: Float = player.distanceTo(enemy)
        var isInExtensionRange: Boolean = distanceToEnemy <= ModuleKillAura.extendedReach

        isPossible = lagging && isTracking && considerVelocityHit && isInExtensionRange

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

    fun addRecentPacketTime() {

        lastPacketTime.add(timer.elapsed)
        timer.reset()

        if (lastPacketTime.size > sampleSize) {
            for (i in 0..<sampleSize) {
                lastPacketTime.removeAt(0)
            }
        }
    }

    fun isLagging(): Boolean {

        if (lastPacketTime.size != sampleSize) {
            return false
        }

        var sumTime: Long = 0

        for (i in 0..<sampleSize) {
            sumTime += lastPacketTime[i]
        }

        var diff: Double = sumTime / sampleSize.toDouble();

        return diff > 0.5
    }

    var isVelocityHitPossible: Boolean = false
        get() = isPossible && super.running
}
