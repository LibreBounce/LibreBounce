package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.util.math.Box

object ModuleProphuntESP : ClientModule("ProphuntESP", Category.RENDER,
    aliases = arrayOf("BlockUpdateDetector", "FallingBlockESP")) {

    private val defaultColor = Color4b(255, 179, 72, 90)

    private val rendererBlockUpdates = PlacementRenderer("RenderBlockUpdates", true, this,
        defaultColor = defaultColor, keep = false
    )

    private object RendererFallingBlock : ToggleableConfigurable(this, "RenderFallingBlockEntity", true) {
        private val colorMode = choices(this, "ColorMode", 0) {
            arrayOf(
                GenericStaticColorMode(it, defaultColor),
                GenericRainbowColorMode(it)
            )
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            renderEnvironmentForWorld(event.matrixStack) {
                drawEntities(event.partialTicks, colorMode.activeChoice, true)
            }
        }
    }

    init {
        tree(rendererBlockUpdates)
        tree(RendererFallingBlock)
    }

    override fun disable() {
        rendererBlockUpdates.clearSilently()
    }

    @Suppress("unused")
    private val networkHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is BlockUpdateS2CPacket -> rendererBlockUpdates.addBlock(packet.pos)
            is ChunkDeltaUpdateS2CPacket -> packet.visitUpdates { pos, _ -> rendererBlockUpdates.addBlock(pos) }
        }
    }

    private fun WorldRenderEnvironment.drawEntities(
        partialTicks: Float,
        colorMode: GenericColorMode<Any>,
        drawOutline: Boolean
    ): Boolean {
        var dirty = false

        BoxRenderer.drawWith(this) {
            world.entities.filterIsInstance<FallingBlockEntity>().map {
                val dimension = it.getDimensions(it.pose)
                val width = dimension.width.toDouble() / 2.0
                it to Box(-width, 0.0, -width, width, dimension.height.toDouble(), width)
            }.forEach { (entity, box) ->
                val pos = entity.interpolateCurrentPosition(partialTicks)
                val color = colorMode.getColor(entity) // which doesn't matter

                val baseColor = color.alpha(50)
                val outlineColor = color.alpha(100)

                withPositionRelativeToCamera(pos) {
                    drawBox(
                        box,
                        baseColor,
                        outlineColor.takeIf { drawOutline }
                    )
                }

                dirty = true
            }
        }

        return dirty
    }
}
