package net.ccbluex.liquidbounce.features.command.commands.client

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.*
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.client.render.RenderLayer
//import net.minecraft.client.render.RenderLayer.getGuiTextured
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.screen.slot.*

object CommandInvsee {

    var viewedPlayer: AbstractClientPlayerEntity? = null

    fun createCommand() = CommandBuilder.begin("invsee")
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

class NoInteractInventory(private var player: PlayerEntity) : InventoryScreen(player) {

    override fun drawBackground(context: DrawContext, delta: Float, mouseX: Int, mouseY: Int) {
        context.drawTexture(
            /* todo: is this proper? */
            RenderLayer::getGuiTextured, BACKGROUND_TEXTURE,
            x, y,
            0F, 0F,
            backgroundWidth, backgroundHeight,
            backgroundWidth, backgroundHeight,
        )
        drawEntity(
            context, x + 26, y + 8, x + 75, y + 78,
            30, 0.0625f, mouseX.toFloat(), mouseY.toFloat(),
            player
        )
    }

    @Suppress("detekt:EmptyFunctionBlock")
    override fun onMouseClick(slot: Slot?, slotId: Int, button: Int, actionType: SlotActionType?) {}
}
