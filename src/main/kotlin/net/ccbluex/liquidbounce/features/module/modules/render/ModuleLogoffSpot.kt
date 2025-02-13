package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.entity.player.PlayerEntity

/**
 * Log off spot
 *
 * Keeps track of the position of each player until they log off or get out of render distance,
 * and when the position is in an unloaded chunk, we remove it from the list.
 */
object ModuleLogoffSpot : ClientModule("LogoffSpot", Category.RENDER) {

    private val lastSeenPlayers = mutableMapOf<Int, WireframePlayer>()
    private val color by color("Color", Color4b(192, 192, 192, 100))
    private var outlineColor by color("OutlineColor", Color4b(192, 192, 192, 255))

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        RenderedEntities.filterIsInstance<PlayerEntity>().forEach { entity ->
            val position = entity.interpolateCurrentPosition(event.partialTicks)
            val rotation = entity.interpolateCurrentRotation(event.partialTicks)
            lastSeenPlayers[entity.id] = WireframePlayer(position, rotation.yaw, rotation.pitch)
        }

        val lastSeenIterator = lastSeenPlayers.iterator()
        while (lastSeenIterator.hasNext()) {
            val (entityId, wireframePlayer) = lastSeenIterator.next()
            val blockPos = wireframePlayer.pos.toBlockPos()

            if (!world.isPosLoaded(blockPos)) {
                lastSeenIterator.remove()
                continue
            }

            if (world.getEntityById(entityId) != null) {
                continue
            }

            wireframePlayer.render(event, color, outlineColor)
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        lastSeenPlayers.clear()
    }

    override fun disable() {
        lastSeenPlayers.clear()
        super.disable()
    }

}
