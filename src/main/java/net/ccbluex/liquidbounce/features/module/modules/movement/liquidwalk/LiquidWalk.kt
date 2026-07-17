/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.aac.AAC
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.aac.AAC3311
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.aac.AACFly
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.other.Vanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.other.Spartan
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.other.Dolphin
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
        mc.player ?: return@handler

        modeModule.onUpdate()
    }

    val onMove = handler<MoveEvent> { event ->
        mc.player ?: return@handler

        modeModule.onMove(event)
    }

    val onPacket = handler<PacketEvent> { event ->
        mc.player ?: return@handler

        modeModule.onPacket(event)
    }

    val onBB = handler<BlockBBEvent> { event ->
        mc.player ?: return@handler

        modeModule.onBB(event)
    }

    val onJump = handler<JumpEvent> { event ->
        mc.player?.run {
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