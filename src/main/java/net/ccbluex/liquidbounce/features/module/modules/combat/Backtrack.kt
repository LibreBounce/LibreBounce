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
import net.minecraft.client.renderer.GlStateManager.color
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.server.*
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.Vec3
import net.minecraft.world.WorldSettings
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object Backtrack : Module("Backtrack", Category.COMBAT) {

    private val nextBacktrackDelay by int("NextBacktrackDelay", 0, 0..2000, suffix = "ms")
    private val delay by intRange("Delay", 80..80, 0..2000, suffix = "ms")

    // Also add an option that stops Backtrack if you can 1-tap your opponent
    // Add a PacketType option, with Sent, Received, and Both modes
    private val style by choices("Style", arrayOf("Pulse", "Smooth"), "Smooth")
    //private val packetMode by choices("PacketMode", arrayOf("Sent", "Received", "Both"), "Both")
    private val distance by floatRange("Distance", 2f..3f, 0f..6f, suffix = "blocks")
    private val smart by boolean("Smart", true)

    private val attackableHurtTime by intRange("AttackableHurtTime", 0..1, 0..10)
    private val flushOnAttackableHurtTime by boolean("FlushOnAttackableHurtTime", false)

    private val flushOnOwnHurtTime by boolean("FlushOnOwnHurtTime", false)
    private val hurtTimeToFlush by intRange("HurtTimeToFlush", 9..10, 0..10) { flushOnOwnHurtTime }

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

    private val packetQueue = ConcurrentLinkedQueue<QueueData>()
    private val positions = ConcurrentLinkedQueue<Pair<Vec3, Long>>()

    var target: EntityLivingBase? = null

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
            is C00Handshake, is C00PacketServerQuery, is S02PacketChat, is S01PacketPong -> return@handler

            is S29PacketSoundEffect -> if (nonDelayedSoundSubstrings in packet.soundName) return@handler

            // Flush on own death
            is S06PacketUpdateHealth -> if (packet.health <= 0) {
                clearPackets()
                return@handler
            }

            is S13PacketDestroyEntities -> if (target != null && target!!.entityId in packet.entityIDs) {
                clearPackets()
                reset()
                return@handler
            }

            is S1CPacketEntityMetadata -> if (target?.entityId == packet.entityId) {
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

            is S19PacketEntityStatus -> if (packet.entityId == target?.entityId) return@handler
        }

        // Cancel every received packet to avoid possible server synchronization issues from random causes.
        if (event.eventType == EventState.RECEIVE) {
            //if (packetMode == "Sent") return@handler

            when (packet) {
                is S14PacketEntity -> if (packet.entityId == target?.entityId) {
                    (target as? IMixinEntity)?.run {
                        positions += Pair(Vec3(trueX, trueY, trueZ), System.currentTimeMillis())
                    }
                }

                is S18PacketEntityTeleport -> if (packet.entityId == target?.entityId) {
                    (target as? IMixinEntity)?.run {
                        positions += Pair(Vec3(trueX, trueY, trueZ), System.currentTimeMillis())
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
                val trueDist = mc.thePlayer.getDistance(targetMixin.trueX, targetMixin.trueY, targetMixin.trueZ)
                val dist = mc.thePlayer.getDistance(target.posX, target.posY, target.posZ)

                if (trueDist <= 6f && (!smart || trueDist > dist) && (style == "Smooth" || !globalTimer.hasTimePassed(
                        supposedDelay
                    ))
                ) {
                    shouldRender = true

                    if (mc.thePlayer.getDistanceToEntityBox(target) in distance) {
                        handlePackets()

                        if (debug && target.hurtTime in attackableHurtTime) chat("(Backtrack) Lag distance: ${dist}, true distance: ${trueDist}")
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

        if (event.targetEntity is EntityLivingBase) {
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
                        val axisAlignedBB = entityBoundingBox.offset(-currPos + Vec3(x, y, z))

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

            if (mc.thePlayer.getDistanceToBox(targetBox) in distance) {
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

    fun <T> runWithSimulatedPosition(entity: Entity, vec3: Vec3, f: () -> T?): T? {
        val currPos = entity.currPos
        val prevPos = entity.prevPos

        entity.setPosAndPrevPos(vec3)

        val result = f()

        // Reset position
        entity.setPosAndPrevPos(currPos, prevPos)

        return result
    }

    fun <T> runWithModifiedRotation(
        entity: EntityPlayer, rotation: Rotation, body: Pair<Float, Float>? = null,
        f: (Rotation) -> T?
    ): T? {
        val currRotation = entity.rotation
        val prevRotation = entity.prevRotation
        val bodyYaw = entity.prevRenderYawOffset to entity.renderYawOffset
        val headRotation = entity.prevRotationYawHead to entity.rotationYawHead

        entity.prevRotation = rotation
        entity.rotation = rotation
        entity.prevRotationYawHead = rotation.yaw
        entity.rotationYawHead = rotation.yaw

        body?.let {
            entity.prevRenderYawOffset = it.first
            entity.renderYawOffset = it.second
        }

        val result = f(rotation)

        entity.rotation = currRotation
        entity.prevRotation = prevRotation
        entity.rotationYawHead = headRotation.second
        entity.prevRotationYawHead = headRotation.first

        body?.let {
            entity.prevRenderYawOffset = bodyYaw.first
            entity.renderYawOffset = bodyYaw.second
        }

        return result
    }

    val color
        get() = espColor.color()

    private fun shouldBacktrack() =
        mc.thePlayer != null && mc.theWorld != null && target != null && mc.thePlayer.health > 0 && (target!!.health > 0 || target!!.health.isNaN()) && mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR && System.currentTimeMillis() >= delayForNextBacktrack && target?.let {
            isSelected(it, true) && (mc.thePlayer?.ticksExisted ?: 0) > 20 && !ignoreWholeTick && (target!!.hurtTime !in attackableHurtTime || !flushOnAttackableHurtTime) && (mc.thePlayer?.hurtTime !in hurtTimeToFlush || !flushOnOwnHurtTime)
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
