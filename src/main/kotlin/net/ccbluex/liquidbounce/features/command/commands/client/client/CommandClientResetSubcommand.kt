package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientResetSubcommand {
    fun resetCommand() = CommandBuilder
        .begin("reset")
        .handler { command, _ ->
            AutoConfig.withLoading {
                ModuleManager
                    // TODO: Remove when HUD no longer contains the Element Configuration
                    .filter { module -> module !is ModuleHud }
                    .forEach { it.restore() }
            }
            chat(regular(command.result("successfullyReset")))
        }
        .build()
}
