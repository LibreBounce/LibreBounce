package net.ccbluex.liquidbounce.features.command.commands.module

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleInventoryTracker
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType

/**
 * Command Invsee
 *
 * ???
 *
 * Module: [ModuleInventoryTracker]
 */
object CommandInvsee : CommandFactory {

    var viewedPlayer: AbstractClientPlayerEntity? = null

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("invsee")
            .requiresIngame()
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .useMinecraftAutoCompletion()
                    .required()
                    .build()
            )
            .handler { command, args ->
                val inputName = (args[0] as String)
                val player = world.players.find { it.nameForScoreboard.equals(inputName, true) }
                    ?: throw CommandException(command.result("playerNotFound", inputName))

                RenderSystem.recordRenderCall {
                    mc.setScreen(NoInteractInventory(player))
                }

                viewedPlayer = player
            }
            .build()
    }

}

class NoInteractInventory(private var player: PlayerEntity) : InventoryScreen(player) {

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(BACKGROUND_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight)
        drawEntity(
            context, x + 26, y + 8, x + 75, y + 78,
            30, 0.0625f, mouseX.toFloat(), mouseY.toFloat(),
            player
        )
    }

    @Suppress("detekt:EmptyFunctionBlock")
    override fun onMouseClick(slot: Slot?, slotId: Int, button: Int, actionType: SlotActionType?) {}
}
