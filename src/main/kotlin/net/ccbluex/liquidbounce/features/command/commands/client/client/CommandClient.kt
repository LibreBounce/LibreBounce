/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.features.command.commands.client.client

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.oauth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.oauth.ClientAccountManager
import net.ccbluex.liquidbounce.api.oauth.OAuthClient
import net.ccbluex.liquidbounce.api.oauth.OAuthClient.startAuth
import net.ccbluex.liquidbounce.config.AutoConfig
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder.Companion.BOOLEAN_VALIDATOR
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.misc.HideAppearance.destructClient
import net.ccbluex.liquidbounce.features.misc.HideAppearance.wipeClient
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.BrowserScreen
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.IntegrationListener.clientJcef
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.integration.theme.component.customComponents
import net.ccbluex.liquidbounce.integration.theme.component.types.ImageComponent
import net.ccbluex.liquidbounce.integration.theme.component.types.TextComponent
import net.ccbluex.liquidbounce.lang.LanguageManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.util.Util

/**
 * Client Command
 *
 * Provides subcommands for client management.
 */
object CommandClient : CommandFactory {

    /**
     * Creates client command with a variety of subcommands.
     */
    override fun createCommand(): Command {
        return CommandBuilder.begin("client")
            .hub()
            .subcommand(CommandClientInfoSubcommand.infoCommand())
            .subcommand(CommandClientBrowserSubcommand.browserCommand())
            .subcommand(CommandClientIntegrationSubcommand.integrationCommand())
            .subcommand(CommandClientLanguageSubcommand.languageCommand())
            .subcommand(CommandClientThemeSubcommand.themeCommand())
            .subcommand(CommandClientComponentSubcommand.componentCommand())
            .subcommand(CommandClientAppearanceSubcommand.appereanceCommand())
            .subcommand(CommandClientPrefixSubcommand.prefixCommand())
            .subcommand(CommandClientDestructSubcommand.destructCommand())
            .subcommand(CommandClientAccountSubcommand.accountCommand())
            .subcommand(CommandClientCosmeticsSubcommand.cosmeticsCommand())
            .subcommand(CommandClientResetSubcommand.resetCommand())
            .build()
    }

}
