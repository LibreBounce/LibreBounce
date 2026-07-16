/*
/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.pos
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.removeEach
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.client.gui.screen.inventory.menu.InventoryMenuScreen
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import kotlin.math.min

object OutboundBacktrack : Module("OutboundBacktrack", Category.COMBAT, gameDetecting = false) {

    private val attackDelay by int("AttackDelay", 500, 0..1000, suffix = "ms")
    private val maxDelay by int("MaxDelay", 550, 0..10000, suffix = "ms")
    private val recoilTime by int("RecoilTime", 750, 0..10000, suffix = "ms")
    
    private val hittableRange by float("HittableRange", 3.04f, 0f..6f, suffix = "blocks")

    private val predictClientMovement by int("PredictClientMovement", 6, 0..10, suffix = "ticks")
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, 0f..10f)

    private val packetQueue = Queues.newArrayDeque<QueueData>()

    private val timeRequiredTimer = MSTimer()
    private val resetTimer = MSTimer()
    private var ignoreWholeTick = false
    private var target: Entity? = null
    private var timeRequired = maxDelay

    override fun onDisable() {
        if (mc.thePlayer == null) return

        blink()
    }

    val onAttack = handler<AttackEvent> { event ->
        target = event.targetEntity ?: return@handler
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        if (!handleEvents() || player.isDead || event.isCancelled || ignoreWholeTick) {
            return@handler
        }

        if (packet !is C02PacketUseEntity || packet.action != ATTACK)
            return@handler

        if (!packet.entityID == target || target == null) return@handler

        val modifiedInput = RotationUtils.modifiedInput
        val simPlayer = SimulatedPlayer.fromClientPlayer(modifiedInput)

        val targetBox = target.hitBox.offset(
            target.currPos.subtract(target.prevPos).times(predictEnemyPosition.toDouble())
        )

        val distance = player.getDistanceToEntityBox(target)

        val (currPos, prevPos) = player.currPos to player.prevPos

        var simDist = player.getDistanceToBox(targetBox)
        var ticksUntilOutOfRange = 0
        var idx = 0

        for (idx in 0 until 8) {
            simPlayer.tick()

            player.setPosAndPrevPos(simPlayer.pos)
            simDist = player.getDistanceToBox(targetBox)
            player.setPosAndPrevPos(currPos, prevPos)

            if (simDist > hittableRange) {
                ticksUntilOutOfRange = idx
                chat("(OutboundBacktrack) Activated outbound backtrack, tries until out of range: $idx")
                break
            }
        }

        val lagCompensatedHurtTime = (target.hurtTime * 50) - player.getPing()
        timeRequired = (attackDelay * 50) - lagCompensatedHurtTime

        chat("(OutboundBacktrack) Lag compensated hurt time: ${lagCompensatedHurtTime}, time required to hit: ${timeRequired}")

        if (lagCompensatedHurtTime <= attackDelay ||
            ticksUntilOutOfRange * 50 > timeRequired ||
            timeRequired > maxDelay / 50
        ) {
            chat("(OutboundBacktrack) Stopped because one of the variables was false")
            blink()
            return@handler
        }

        if (!resetTimer.hasTimePassed(recoilTime)) return@handler

        if (mc.isSingleplayer || mc.currentServerData == null) {
            blink()
            return@handler
        }

        if (event.eventType == EventState.SEND) {
            event.cancelEvent()

            synchronized(packetQueue) {
                packetQueue += QueueData(packet, System.currentTimeMillis())
            }
        }
    }

    val onWorld = handler<WorldEvent> { event ->
        // Clear packets on disconnect only
        if (event.worldClient == null) blink(false)
    }

    private fun getTruePositionEyes(player: EntityPlayer): Vec3d {
        val mixinPlayer = player as? IMixinEntity

        return Vec3d(mixinPlayer!!.trueX, mixinPlayer.trueY + player.getEyeHeight().toDouble(), mixinPlayer.trueZ)
    }

    val onGameLoop = handler<GameLoopEvent> {
        val player = mc.thePlayer ?: return@handler
        mc.theWorld ?: return@handler

        if (Blink.blinkingSend() || player.isDead) {
            blink()
            return@handler
        }

        if (timeRequiredTimer.hasTimePassed(timeRequired)) {
            blink()

            return@handler
        }

        if (!resetTimer.hasTimePassed(recoilTime)) return@handler

        handlePackets()
        ignoreWholeTick = false
    }

    override val tag
        get() = packetQueue.size.toString()

    private fun blink(handlePackets: Boolean = true) {
        mc.addScheduledTask {
            if (handlePackets) {
                resetTimer.reset()
            }

            timeRequiredTimer.reset()

            handlePackets(true)
            ignoreWholeTick = true
        }
    }

    private fun handlePackets(clear: Boolean = false) {
        synchronized(packetQueue) {
            packetQueue.removeEach { (packet, timestamp) ->
                if (timestamp <= System.currentTimeMillis() - delay || clear) {
                    sendPacket(packet, false)
                    true
                } else false
            }
        }
    }
}*/