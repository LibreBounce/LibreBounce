package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.entity.FallingBlockEntity
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket

object ModuleProphuntESP : ClientModule("ProphuntESP", Category.RENDER,
    aliases = arrayOf("BlockUpdateDetector", "FallingBlockESP")) {

    private val defaultColor = Color4b(255, 179, 72, 90)

    private val renderer = PlacementRenderer("RenderBlockUpdates", true, this,
        defaultColor = defaultColor, keep = false
    )

    init {
        tree(renderer)
    }

    override fun disable() {
        renderer.clearSilently()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        world.entities.filterIsInstance<FallingBlockEntity>().forEach {
            renderer.addBlock(it.blockPos)
        }
    }

    @Suppress("unused")
    private val networkHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is BlockUpdateS2CPacket -> renderer.addBlock(packet.pos)
            is ChunkDeltaUpdateS2CPacket -> packet.visitUpdates { pos, _ ->
                renderer.addBlock(pos.toImmutable())
            }
        }
    }
}
