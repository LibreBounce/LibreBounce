package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.init.Blocks.*
import net.minecraft.util.AxisAlignedBB

object AvoidHazards : Module("AvoidHazards", Category.WORLD) {
    private val onFire by boolean("Fire", true)
    private val onCobweb by boolean("Cobweb", true)
    private val onCactus by boolean("Cactus", true)
    private val onLava by boolean("Lava", true)
    private val onWater by boolean("Water", true)
    private val plate by boolean("PressurePlate", true)
    private val onSnow by boolean("Snow", true)

    val onBlockBB = handler<BlockBBEvent> { e ->
        val player = mc.thePlayer ?: return@handler

        when (e.block) {
            fire -> if (!onFire) return@handler

            web -> if (!onCobweb) return@handler

            snow -> if (!onSnow) return@handler

            cactus -> if (!onCactus) return@handler

            water, flowing_water ->
                // Don't prevent water from cancelling fall damage.
                if (!onWater || player.fallDistance >= 3.34627 || player.isInWater) return@handler

            lava, flowing_lava -> if (!lava) return@handler

            wooden_pressure_plate, stone_pressure_plate, light_weighted_pressure_plate, heavy_weighted_pressure_plate -> {
                if (plate)
                    e.boundingBox =
                        AxisAlignedBB(e.x.toDouble(), e.y.toDouble(), e.z.toDouble(), e.x + 1.0, e.y + 0.25, e.z + 1.0)
                return@handler
            }

            else -> return@handler
        }

        e.boundingBox = AxisAlignedBB(e.x.toDouble(), e.y.toDouble(), e.z.toDouble(), e.x + 1.0, e.y + 1.0, e.z + 1.0)
    }
}