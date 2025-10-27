/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.aac.AAC
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.aac.AAC3311
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.aac.AACFly
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.other.Vanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.other.Spartan
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalkmodes.other.Dolphin
import net.ccbluex.liquidbounce.utils.block.block
import net.minecraft.block.BlockLiquid
import net.minecraft.util.BlockPos
import org.lwjgl.input.Keyboard

object LiquidWalk : Module("LiquidWalk", Category.MOVEMENT, Keyboard.KEY_J) {

    private val liquidWalkModes = arrayOf(
        // Main
        Vanilla, NCP,

        // AAC
        AAC, AAC3311, AACFly,

        // Other
        Spartan, Dolphin
    )

    private val modes = liquidWalkModes.map { it.modeName }.toTypedArray()

    val mode by choices("Mode", modes, "NCP")
    val aacFly by float("AACFlyMotion", 0.5f, 0.1f..1f) { mode == "AACFly" }

    private val noJump by boolean("NoJump", false)

    val onUpdate = handler<UpdateEvent> { event ->
        mc.thePlayer ?: return@handler

        modeModule.onUpdate()
    }

    val onMove = handler<MoveEvent> { event ->
        mc.thePlayer ?: return@handler

        modeModule.onMove(event)
    }

    val onPacket = handler<PacketEvent> { event ->
        mc.thePlayer ?: return@handler

        modeModule.onPacket(event)
    }

    val onBB = handler<BlockBBEvent> { event ->
        mc.thePlayer ?: return@handler

        modeModule.onBB(event)
    }

    val onJump = handler<JumpEvent> { event ->
        mc.thePlayer?.run {
            val block = BlockPos(posX, posY - 0.01, posZ).block

            if (noJump && block is BlockLiquid)
                event.cancelEvent()
        }
    }

    override val tag
        get() = mode

    private val modeModule
        get() = liquidWalkModes.find { it.modeName == mode }!!
}