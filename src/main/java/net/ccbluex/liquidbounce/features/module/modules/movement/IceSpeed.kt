/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.minecraft.init.Blocks.air
import net.minecraft.init.Blocks.ice
import net.minecraft.init.Blocks.packed_ice
import net.minecraft.util.BlockPos

object IceSpeed : Module("IceSpeed", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("Vanilla", "NCP", "AAC", "Spartan"), "NCP")
    private val speed by float("Speed", 0.5f, 0.2f..1.6f) { mode == "Vanilla" }
    private val iceSlipperiness by float("IceSlipperiness", 0.98f, 0.01f..1f) { mode == "Vanilla" }
    private val packedIceSlipperiness by float("PackedIceSlipperiness", 0.98f, 0.01f..1f) { mode == "Vanilla" }

    override fun onEnable() {
        if (mode == "NCP") {
            ice.slipperiness = 0.39f
            packed_ice.slipperiness = 0.39f
        }
        super.onEnable()
    }

    val onUpdate = handler<UpdateEvent> {
        val mode = mode

        when (mode) {
            "Vanilla" -> {
                ice.slipperiness = iceSlipperiness
                packed_ice.slipperiness = packedIceSlipperiness
            }
            "NCP" -> {
                ice.slipperiness = 0.39f
                packed_ice.slipperiness = 0.39f
            }
            else -> {
                ice.slipperiness = 0.98f
                packed_ice.slipperiness = 0.98f
            }
        }

        val player = mc.thePlayer ?: return@handler

        if (!player.onGround || player.isOnLadder || player.isSneaking || !player.isSprinting || !player.isMoving) {
            return@handler
        }

        if (player.position.down().block.let { it != ice && it != packed_ice }) {
            return@handler
        }

        when (mode) {
            "AAC" -> {
                player.motionX *= 1.342
                player.motionZ *= 1.342
                ice.slipperiness = 0.6f
                packed_ice.slipperiness = 0.6f
            }

            "Spartan" -> {
                val upBlock = BlockPos(player).up(2).block

                if (upBlock != air) {
                    player.motionX *= 1.342
                    player.motionZ *= 1.342
                } else {
                    player.motionX *= 1.18
                    player.motionZ *= 1.18
                }

                ice.slipperiness = 0.6f
                packed_ice.slipperiness = 0.6f
            }

            "Vanilla" -> {
                player.motionX *= speed
                player.motionZ *= speed
            }
        }
    }

    override fun onDisable() {
        ice.slipperiness = 0.98f
        packed_ice.slipperiness = 0.98f
        super.onDisable()
    }
}