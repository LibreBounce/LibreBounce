package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular

object CommandClientCosmeticsSubcommand {
    fun cosmeticsCommand() = CommandBuilder
        .begin("cosmetics")
        .hub()
        .subcommand(refreshSubcommand())
        .subcommand(manageSubcommand())
        .build()

    private fun manageSubcommand() = CommandBuilder.begin("manage")
        .handler { _, _ ->
            browseUrl("https://user.liquidbounce.net/cosmetics")
        }
        .build()

    private fun refreshSubcommand() = CommandBuilder.begin("refresh")
        .handler { _, _ ->
            chat(
                regular(
                    "Refreshing cosmetics..."
                )
            )
            CosmeticService.carriersCosmetics.clear()
            ClientAccountManager.clientAccount.cosmetics = null

            CosmeticService.refreshCarriers(true) {
                chat(
                    regular(
                        "Cosmetic System has been refreshed."
                    )
                )
            }
        }
        .build()
}
