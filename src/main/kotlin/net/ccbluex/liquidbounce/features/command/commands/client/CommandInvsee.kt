package net.ccbluex.liquidbounce.features.command.commands.client

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.*
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleInventoryTracker
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.inventory.ViewedInventoryScreen
import java.util.*

object CommandInvsee {

    var viewedPlayer: UUID? = null

    fun createCommand() = CommandBuilder.begin("invsee")
        .parameter(
            playerParameter()
                .required()
                .build()
        )
        .handler { command, args ->
            val inputName = args[0] as String
            val playerID = network.playerList.find { it.profile.name.equals(inputName, true) }?.profile?.id
            val player = { world.getPlayerByUuid(playerID) ?: ModuleInventoryTracker.playerMap[playerID] }

            if (playerID == null || player() == null) {
                throw CommandException(command.result("playerNotFound", inputName))
            }

            RenderSystem.recordRenderCall {
                mc.setScreen(ViewedInventoryScreen(player))
            }
            viewedPlayer = playerID
        }
        .build()
}
