/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.DelayData
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.drawSolidBox
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squareBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.entity.Entity
import net.minecraft.entity.TrackedPosition
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object ModuleBacktrack : Module("Backtrack", Category.COMBAT) {

    private val range by floatRange("Range", 1f..3f, 0f..6f)
    private val delay by intRange("Delay", 100..150, 0..1000, "ms")
    private val nextBacktrackDelay by intRange("NextBacktrackDelay", 0..10, 0..2000, "ms")
    private val chance by float("Chance", 50f, 0f..100f, "%")
    val renderMode = choices("RenderMode", Box, arrayOf(Box, Model, Wireframe, None))

    private val packetQueue = LinkedHashSet<DelayData>()
    private val chronometer = Chronometer()

    private var target: Entity? = null
    private var position: TrackedPosition? = null

    val packetHandler = handler<PacketEvent> {
        if (packetQueue.isNotEmpty()) {
            chronometer.waitForAtLeast(nextBacktrackDelay.random().toLong())
        }

        synchronized(packetQueue) {
            if (it.origin != TransferOrigin.RECEIVE || it.isCancelled) {
                return@handler
            }

            if (packetQueue.isEmpty() && !shouldCancelPackets()) {
                return@handler
            }

            val packet = it.packet

            when (packet) {
                // Ignore message-related packets
                is ChatMessageC2SPacket, is GameMessageS2CPacket, is CommandExecutionC2SPacket -> {
                    return@handler
                }

                // Flush on teleport or disconnect
                is PlayerPositionLookS2CPacket, is DisconnectS2CPacket -> {
                    clear(true)
                    return@handler
                }

                // Ignore own hurt sounds
                is PlaySoundS2CPacket -> {
                    if (packet.sound.value() == SoundEvents.ENTITY_PLAYER_HURT) {
                        return@handler
                    }
                }

                // Flush on own death
                is HealthUpdateS2CPacket -> {
                    if (packet.health <= 0) {
                        clear(true)
                        return@handler
                    }
                }
            }

            // Update box position with these packets
            val entityPacket = packet is EntityS2CPacket && packet.getEntity(world) == target
            val positionPacket = packet is EntityPositionS2CPacket && packet.entityId == target?.id
            if (entityPacket || positionPacket) {
                val pos = if (packet is EntityS2CPacket) {
                    position?.withDelta(packet.deltaX.toLong(), packet.deltaY.toLong(), packet.deltaZ.toLong())
                } else {
                    (packet as EntityPositionS2CPacket).let { vec -> Vec3d(vec.x, vec.y, vec.z) }
                }

                position?.setPos(pos)

                // Is the target's actual position closer than its tracked position?
                if (target!!.squareBoxedDistanceTo(player, pos!!) < target!!.squaredBoxedDistanceTo(player)) {
                    // Process all packets. We want to be able to hit the enemy, not the opposite.
                    processPackets(true)
                    // And stop right here. No need to cancel further packets.
                    return@handler
                }
            }

            it.cancelEvent()

            packetQueue.add(DelayData(packet, System.currentTimeMillis()))
        }
    }

    abstract class RenderChoice(name: String) : Choice(name) {
        protected fun getEntityPosition(): Pair<Entity, Vec3d>? {
            val entity = target ?: return null
            val pos = position?.pos ?: return null
            return entity to pos
        }
    }

    object Box : RenderChoice("Box") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = renderMode

        private val color by color("Color", Color4b(36, 32, 147, 87))

        val renderHandler = handler<WorldRenderEvent> { event ->
            val (entity, pos) = getEntityPosition() ?: return@handler

            val dimensions = entity.getDimensions(entity.pose)
            val d = dimensions.width.toDouble() / 2.0

            val box = Box(-d, 0.0, -d, d, dimensions.height.toDouble(), d).expand(0.05)

            renderEnvironmentForWorld(event.matrixStack) {
                withPositionRelativeToCamera(pos) {
                    withColor(color) {
                        drawSolidBox(box)
                    }
                }
            }
        }
    }

    object Model : RenderChoice("Model") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = renderMode

        private val lightAmount by float("LightAmount", 0.3f, 0.01f..1f)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val (entity, pos) = getEntityPosition() ?: return@handler

            val light = world.getLightLevel(BlockPos.ORIGIN)
            val reducedLight = (light * lightAmount.toDouble()).toInt()

            renderEnvironmentForWorld(event.matrixStack) {
                withPositionRelativeToCamera(pos) {
                    mc.entityRenderDispatcher.render(
                        entity,
                        0.0,
                        0.0,
                        0.0,
                        entity.yaw,
                        1.0f,
                        event.matrixStack,
                        mc.bufferBuilders.entityVertexConsumers,
                        reducedLight
                    )
                }
            }
        }
    }

    object Wireframe : RenderChoice("Wireframe") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = renderMode

        private val color by color("Color", Color4b(36, 32, 147, 87))
        private val outlineColor by color("OutlineColor", Color4b(36, 32, 147, 255))

        val renderHandler = handler<WorldRenderEvent> {
            val (entity, pos) = getEntityPosition() ?: return@handler

            val wireframePlayer = WireframePlayer(pos, entity.yaw, entity.pitch)
            wireframePlayer.render(it, color, outlineColor)
        }
    }

    object None : RenderChoice("None") {
        override val parent: ChoiceConfigurable<RenderChoice>
            get() = renderMode
    }

    /**
     * When we process packets, we must imitate the game's server-packet handling logic
     * This means the module MUST have top priority.
     *
     * @see net.minecraft.client.MinecraftClient.render
     *
     * Runnable runnable;
     * while((runnable = (Runnable)this.renderTaskQueue.poll()) != null) {
     *      runnable.run();
     * }
     *
     * That gets called first, then the client's packets.
     */
    val tickHandler = handler<GameTickEvent>(priority = 1002) {
        if (shouldCancelPackets()) {
            processPackets()
        } else {
            clear()
        }
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        // Clear packets on disconnect only
        if (it.world == null) {
            clear(clearOnly = true)
        }
    }

    @Suppress("unused")
    val attackHandler = handler<AttackEvent> {
        val enemy = it.enemy

        if (!shouldBacktrack(enemy))
            return@handler

        // Reset on enemy change
        if (enemy != target) {
            clear()

            // Instantly set new position, so it does not look like the box was created with delay
            position = TrackedPosition().apply { this.pos = enemy.trackedPosition.pos }
        }

        target = enemy
    }

    override fun enable() {
        clear(false)
    }

    override fun disable() {
        clear(true)
    }

    private fun processPackets(clear: Boolean = false) {
        synchronized(packetQueue) {
            packetQueue.removeIf {
                if (clear || it.delay <= System.currentTimeMillis() - delay.random()) {
                    mc.renderTaskQueue.add { handlePacket(it.packet) }
                    return@removeIf true
                }
                false
            }
        }
    }

    fun clear(handlePackets: Boolean = true, clearOnly: Boolean = false) {
        if (handlePackets && !clearOnly) {
            processPackets(true)
        } else if (clearOnly) {
            synchronized(packetQueue) {
                packetQueue.clear()
            }
        }

        target = null
        position = null
    }

    fun isLagging() =
        enabled && packetQueue.isNotEmpty()

    private fun shouldBacktrack(target: Entity) =
        target.shouldBeAttacked() &&
            target.boxedDistanceTo(player) in range &&
            player.age > 10 &&
            Math.random() * 100 < chance &&
            chronometer.hasElapsed()

    private fun shouldCancelPackets() =
        target != null && target!!.isAlive && shouldBacktrack(target!!)
}
