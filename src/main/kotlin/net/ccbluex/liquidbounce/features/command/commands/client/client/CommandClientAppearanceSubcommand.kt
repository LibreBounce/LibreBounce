package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientAppearanceSubcommand {
    fun appereanceCommand() = CommandBuilder.begin("appearance")
        .hub()
        .subcommand(hideSubcommand())
        .subcommand(showSubcommand())
        .build()

    private fun showSubcommand() = CommandBuilder.begin("show")
        .handler { command, _ ->
            if (!HideAppearance.isHidingNow) {
                chat(regular(command.result("alreadyShowingAppearance")))
                return@handler
            }

            chat(regular(command.result("showingAppearance")))
            HideAppearance.isHidingNow = false
        }.build()

    private fun hideSubcommand() = CommandBuilder.begin("hide")
        .handler { command, _ ->
            if (HideAppearance.isHidingNow) {
                chat(regular(command.result("alreadyHidingAppearance")))
                return@handler
            }

            chat(regular(command.result("hidingAppearance")))
            HideAppearance.isHidingNow = true
        }.build()
}
