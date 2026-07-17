/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.realX
import net.ccbluex.liquidbounce.utils.client.realY
import net.ccbluex.liquidbounce.utils.client.realZ
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils.contains
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBacktrackBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.minecraft.client.render.platform.GlStateManager.color
import net.minecraft.entity.Entity
import net.minecraft.entity.living.LivingEntity
import net.minecraft.entity.living.player.PlayerEntity
import net.minecraft.network.Packet
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.network.packet.c2s.query.ServerStatusC2SPacket
import net.minecraft.network.packet.s2c.query.PingS2CPacket
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldSettings
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object Backtrack : Module("Backtrack", Category.COMBAT) {

    private val nextBacktrackDelay by int("NextBacktrackDelay", 0, 0..10000, suffix = "ms")
    private val delay by intRange("Delay", 80..80, 0..10000, suffix = "ms")

    // Also add an option that stops Backtrack if you can 1-tap your opponent
    // Add a PacketType option, with Sent, Received, and Both modes
    private val style by choices("Style", arrayOf("Pulse", "Smooth"), "Smooth")
    //private val packetMode by choices("PacketMode", arrayOf("Sent", "Received", "Both"), "Both")
    private val distance by floatRange("Distance", 2f..3f, 0f..6f, suffix = "blocks")
    private val smart by boolean("Smart", true)
    private val advantageTreshold by float("AdvantageTreshold", 0f, 0f..1f, suffix = "blocks") { smart }

    private val targetHurtTimeHandling by choices("TargetHurtTimeHandling", arrayOf("Allow", "Forbid", "Ignore"), "Ignore")
    private val targetHurtTime by intRange("TargetHurtTime", 0..1, 0..10) { targetHurtTimeHandling != "Ignore" }

    private val ownHurtTimeHandling by choices("OwnHurtTimeHandling", arrayOf("Allow", "Forbid", "Ignore"), "Ignore")
    private val ownHurtTime by intRange("OwnHurtTime", 9..10, 0..10) { ownHurtTimeHandling != "Ignore" }

    // ESP
    private val espMode by choices(
        "ESPMode",
        arrayOf("None", "Box", "Model", "Wireframe"),
        "Box"
    ).subjective()
    private val wireframeWidth by float("WireframeWidth", 1f, 0.5f..5f) { espMode == "Wireframe" }.subjective()

    private val espColor =
        ColorSettingsInteger(this, "ESPColor") { espMode != "Model" }.with(0, 255, 0)

    private val debug by boolean("Debug", false).subjective()
    private val targetHurtTimeToDebug by intRange("TargetHurtTimeToDebug", 0..1, 0..10) { debug }

    private val packetQueue = ConcurrentLinkedQueue<QueueData>()
    private val positions = ConcurrentLinkedQueue<Pair<Vec3d, Long>>()

    var target: LivingEntity? = null

    private var globalTimer = MSTimer()

    var shouldRender = true

    private var ignoreWholeTick = false

    private var delayForNextBacktrack = 0L

    private var modernDelay = delay.random() to false

    private val supposedDelay
        get() = modernDelay.first

    private val nonDelayedSoundSubstrings = arrayOf("game.player.hurt", "game.player.die")

    val isPacketQueueEmpty
        get() = packetQueue.isEmpty()

    val areQueuedPacketsEmpty
        get() = PacketUtils.isQueueEmpty()

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (TickBase.duringTickModification) {
            clearPackets(stopRendering = false)
            return@handler
        }

        if (Blink.blinkingReceive() || event.isCancelled) return@handler

        if (mc.isSingleplayer || mc.currentServerData == null) {
            clearPackets()
            return@handler
        }

        // Prevent cancelling packets when not needed
        if (isPacketQueueEmpty && areQueuedPacketsEmpty && !shouldBacktrack()) return@handler

        //if (packetMode == "Received") return@handler

        when (packet) {
            // Ignore server related packets
            is HandshakeC2SPacket, is ServerStatusC2SPacket, is ChatMessageS2CPacket, is PingS2CPacket -> return@handler

            is SoundEventS2CPacket -> if (nonDelayedSoundSubstrings in packet.soundName) return@handler

            // Flush on own death
            is PlayerHealthS2CPacket -> if (packet.health <= 0) {
                clearPackets()
                return@handler
            }

            is RemoveEntitiesS2CPacket -> if (target != null && target!!.entityId in packet.entityIDs) {
                clearPackets()
                reset()
                return@handler
            }

            is EntityDataS2CPacket -> if (target?.entityId == packet.entityId) {
                val metadata = packet.func_149376_c() ?: return@handler

                metadata.forEach {
                    if (it.dataValueId == 6) {
                        val objectValue = it.getObject().toString().toDoubleOrNull()
                        if (objectValue != null && !objectValue.isNaN() && objectValue <= 0.0) {
                            clearPackets()
                            reset()
                            return@handler
                        }
                    }
                }

                return@handler
            }

            is EntityEventS2CPacket -> if (packet.entityId == target?.entityId) return@handler
        }

        // Cancel every received packet to avoid possible server synchronization issues from random causes.
        if (event.eventType == EventState.RECEIVE) {
            //if (packetMode == "Sent") return@handler

            when (packet) {
                is EntityMoveS2CPacket -> if (packet.entityId == target?.entityId) {
                    (target as? IMixinEntity)?.run {
                        positions += Pair(Vec3d(trueX, trueY, trueZ), System.currentTimeMillis())
                    }
                }

                is EntityTeleportS2CPacket -> if (packet.entityId == target?.entityId) {
                    (target as? IMixinEntity)?.run {
                        positions += Pair(Vec3d(trueX, trueY, trueZ), System.currentTimeMillis())
                    }
                }
            }

            event.cancelEvent()
            packetQueue += QueueData(packet, System.currentTimeMillis())
        }
    }

    val onGameLoop = handler<GameLoopEvent> {
        val target = target
        val targetMixin = target as? IMixinEntity

        if (shouldBacktrack() && targetMixin != null) {
            if (!Blink.blinkingReceive() && targetMixin.truePos) {
                val trueDist = mc.player.getDistance(targetMixin.trueX, targetMixin.trueY, targetMixin.trueZ)
                val dist = mc.player.getDistance(target.posX, target.posY, target.posZ)

                if (trueDist <= 6f && (!smart || trueDist > dist + advantageTreshold) && (style == "Smooth" || !globalTimer.hasTimePassed(
                        supposedDelay
                    ))
                ) {
                    shouldRender = true

                    if (mc.player.getDistanceToEntityBox(target) in distance) {
                        handlePackets()

                        if (debug && target.damagedTimer in targetHurtTimeToDebug) chat("(Backtrack) Lag distance: ${dist}, true distance: ${trueDist}")
                    } else {
                        handlePacketsRange()
                    }
                } else clear()
            }
        } else clear()

        ignoreWholeTick = false
    }

    /**
     * Priority lower than [PacketUtils] GameLoopEvent function's priority.
     */
    val onQueuePacketClear = handler<GameLoopEvent>(priority = -6) {
        val shouldChangeDelay = isPacketQueueEmpty && areQueuedPacketsEmpty

        if (!shouldChangeDelay) {
            modernDelay = modernDelay.first to false
        }

        if (shouldChangeDelay && !modernDelay.second && !shouldBacktrack()) {
            delayForNextBacktrack = System.currentTimeMillis() + nextBacktrackDelay
            modernDelay = delay.random() to true
        }
    }

    val onAttack = handler<AttackEvent> { event ->
        if (!isSelected(event.targetEntity, true)) return@handler

        // Clear all packets, start again on enemy change
        if (target != event.targetEntity) {
            clearPackets()
            reset()
        }

        if (event.targetEntity is LivingEntity) {
            target = event.targetEntity
        }
    }

    val onRender3D = handler<Render3DEvent> { event ->
        val manager = mc.renderManager ?: return@handler

        if (!shouldBacktrack() || !shouldRender) return@handler

        target?.run {
            val targetEntity = target as IMixinEntity

            val (x, y, z) = targetEntity.interpolatedPosition - manager.renderPos

            if (targetEntity.truePos) {
                when (espMode) {
                    "Box" -> {
                        val axisAlignedBB = shape.offset(-currPos + Vec3d(x, y, z))

                        drawBacktrackBox(axisAlignedBB, color)
                    }

                    "Model" -> {
                        glPushMatrix()
                        glPushAttrib(GL_ALL_ATTRIB_BITS)

                        color(0.6f, 0.6f, 0.6f, 1f)
                        manager.doRenderEntity(
                            this,
                            x,
                            y,
                            z,
                            prevRotationYaw + (rotationYaw - prevRotationYaw) * event.partialTicks,
                            event.partialTicks,
                            true
                        )

                        glPopAttrib()
                        glPopMatrix()
                    }

                    "Wireframe" -> {
                        glPushMatrix()
                        glPushAttrib(GL_ALL_ATTRIB_BITS)

                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)
                        glDisable(GL_TEXTURE_2D)
                        glDisable(GL_LIGHTING)
                        glDisable(GL_DEPTH_TEST)
                        glEnable(GL_LINE_SMOOTH)

                        glEnable(GL_BLEND)
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                        glLineWidth(wireframeWidth)

                        glColor(color)
                        manager.doRenderEntity(
                            this,
                            x,
                            y,
                            z,
                            prevRotationYaw + (rotationYaw - prevRotationYaw) * event.partialTicks,
                            event.partialTicks,
                            true
                        )
                        glColor(color)
                        manager.doRenderEntity(
                            this,
                            x,
                            y,
                            z,
                            prevRotationYaw + (rotationYaw - prevRotationYaw) * event.partialTicks,
                            event.partialTicks,
                            true
                        )

                        glPopAttrib()
                        glPopMatrix()
                    }
                }
            }
        }
    }

    val onWorld = handler<WorldEvent> { event ->
        // Clear packets on disconnect only
        // Set target to null on world change
        if (event.worldClient == null) clearPackets(false)
        target = null
    }

    override fun onEnable() = reset()

    override fun onDisable() {
        clearPackets()
    }

    private fun handlePackets() {
        packetQueue.removeAll { (packet, timestamp) ->
            if (timestamp <= System.currentTimeMillis() - supposedDelay) {
                PacketUtils.schedulePacketProcess(packet)
                true
            } else false
        }

        positions.removeAll { (_, timestamp) -> timestamp < System.currentTimeMillis() - supposedDelay }
    }

    private fun handlePacketsRange() {
        val time = getRangeTime()

        if (time == -1L) {
            clearPackets()
            return
        }

        packetQueue.removeAll { (packet, timestamp) ->
            if (timestamp <= time) {
                PacketUtils.schedulePacketProcess(packet)
                true
            } else false
        }

        positions.removeAll { (_, timestamp) -> timestamp < time }
    }

    private fun getRangeTime(): Long {
        val target = this.target ?: return 0L

        var time = 0L
        var found = false

        for (data in positions) {
            time = data.second

            val targetPos = target.currPos

            val targetBox = target.hitBox.offset(data.first - targetPos)

            if (mc.player.getDistanceToBox(targetBox) in distance) {
                found = true
                break
            }
        }

        return if (found) time else -1L
    }

    private fun clearPackets(handlePackets: Boolean = true, stopRendering: Boolean = true) {
        packetQueue.removeAll {
            if (handlePackets) {
                PacketUtils.schedulePacketProcess(it.packet)
            }

            true
        }

        positions.clear()

        if (stopRendering) {
            shouldRender = false
            ignoreWholeTick = true
        }
    }

    fun <T> runWithNearestTrackedDistance(entity: Entity, f: () -> T): T {
        return f()
    }

    fun <T> runWithSimulatedPosition(entity: Entity, vec3: Vec3d, f: () -> T?): T? {
        val currPos = entity.currPos
        val prevPos = entity.prevPos

        entity.setPosAndPrevPos(vec3)

        val result = f()

        // Reset position
        entity.setPosAndPrevPos(currPos, prevPos)

        return result
    }

    fun <T> runWithModifiedRotation(
        entity: PlayerEntity, rotation: Rotation, body: Pair<Float, Float>? = null,
        f: (Rotation) -> T?
    ): T? {
        val currRotation = entity.rotation
        val prevRotation = entity.prevRotation
        val bodyYaw = entity.lastBodyYaw to entity.bodyYaw
        val headRotation = entity.lastHeadYaw to entity.headYaw

        entity.prevRotation = rotation
        entity.rotation = rotation
        entity.lastHeadYaw = rotation.yaw
        entity.headYaw = rotation.yaw

        body?.let {
            entity.lastBodyYaw = it.first
            entity.bodyYaw = it.second
        }

        val result = f(rotation)

        entity.rotation = currRotation
        entity.prevRotation = prevRotation
        entity.headYaw = headRotation.second
        entity.lastHeadYaw = headRotation.first

        body?.let {
            entity.lastBodyYaw = bodyYaw.first
            entity.bodyYaw = bodyYaw.second
        }

        return result
    }

    val color
        get() = espColor.color()

    private fun onAllowedHurtTime(): Boolean {
        val playerAllowed = when (ownHurtTimeHandling) {
                "Allow" -> mc.player!!.damagedTimer in ownHurtTime
                "Forbid" -> mc.player!!.damagedTimer !in ownHurtTime
                else -> true
            }

        val targetAllowed = when (targetHurtTimeHandling) {
                "Allow" -> target!!.damagedTimer in targetHurtTime
                "Forbid" -> target!!.damagedTimer !in targetHurtTime
                else -> true
            }

        return playerAllowed && targetAllowed
    }

    private fun shouldBacktrack() =
        mc.player != null && mc.world != null && target != null && mc.player.health > 0 && (target!!.health > 0 || target!!.health.isNaN()) && mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR && System.currentTimeMillis() >= delayForNextBacktrack && target?.let {
            isSelected(it, true) && (mc.player?.ticksExisted ?: 0) > 20 && !ignoreWholeTick && onAllowedHurtTime()
        } == true

    private fun reset() {
        target = null
        globalTimer.reset()
    }

    private fun clear() {
        clearPackets()
        globalTimer.reset()
    }

    override val tag: String
        get() = supposedDelay.toString()
}

data class QueueData(val packet: Packet<*>, val time: Long)
data class BacktrackData(val x: Double, val y: Double, val z: Double, val time: Long)
