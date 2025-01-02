package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.services.auth.OAuthClient
import net.ccbluex.liquidbounce.api.services.auth.OAuthClient.startAuth
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.util.Util

object CommandClientAccountSubcommand {
    fun accountCommand() = CommandBuilder.begin("account")
        .hub()
        .subcommand(loginSubcommand())
        .subcommand(logoutSubcommand())
        .subcommand(infoSubcommand())
        .build()

    private fun infoSubcommand() = CommandBuilder.begin("info")
        .handler { _, _ ->
            if (ClientAccountManager.clientAccount == EMPTY_ACCOUNT) {
                chat(regular("You are not logged in."))
                return@handler
            }

            chat(regular("Getting user information..."))
            OAuthClient.runWithScope {
                runCatching {
                    val account = ClientAccountManager.clientAccount
                    account.updateInfo()
                    account
                }.onSuccess { account ->
                    account.userInformation?.let { info ->
                        chat(regular("User ID: "), variable(info.userId))
                        chat(regular("Donation Perks: "), variable(if (info.premium) "Yes" else "No"))
                    }
                }.onFailure {
                    chat(markAsError("Failed to get user information: ${it.message}"))
                }

            }
        }.build()

    private fun logoutSubcommand() = CommandBuilder.begin("logout")
        .handler { _, _ ->
            if (ClientAccountManager.clientAccount == EMPTY_ACCOUNT) {
                chat(regular("You are not logged in."))
                return@handler
            }

            chat(regular("Logging out..."))
            OAuthClient.runWithScope {
                ClientAccountManager.clientAccount = EMPTY_ACCOUNT
                ConfigSystem.storeConfigurable(ClientAccountManager)
                chat(regular("Successfully logged out."))
            }
        }.build()

    private fun loginSubcommand() = CommandBuilder.begin("login")
        .handler { _, _ ->
            if (ClientAccountManager.clientAccount != EMPTY_ACCOUNT) {
                chat(regular("You are already logged in."))
                return@handler
            }

            chat(regular("Starting OAuth authorization process..."))
            OAuthClient.runWithScope {
                val account = startAuth { Util.getOperatingSystem().open(it) }
                ClientAccountManager.clientAccount = account
                ConfigSystem.storeConfigurable(ClientAccountManager)
                chat(regular("Successfully authorized client."))
            }
        }.build()
}
