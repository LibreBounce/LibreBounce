package net.ccbluex.liquidbounce.features.command.commands.client.client

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.integration.BrowserScreen
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientBrowserSubcommand {
    fun browserCommand() = CommandBuilder.begin("browser")
        .hub()
        .subcommand(openSubcommand())
        .build()

    private fun openSubcommand() = CommandBuilder.begin("open")
        .parameter(
            ParameterBuilder.begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .build()
        ).handler { command, args ->
            chat(regular("Opening browser..."))
            RenderSystem.recordRenderCall {
                mc.setScreen(BrowserScreen(args[0] as String))
            }
        }.build()
}
