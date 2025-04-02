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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace.item

import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount.Companion.EMPTY_ACCOUNT
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.cosmetic.ClientAccountManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Delete marketplace item
 */
object DeleteItemCommand : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("delete")
        .parameter(
            ParameterBuilder
                .begin<Int>("id")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .required()
                .build()
        )
        .handler { command, args ->
            val clientAccount = ClientAccountManager.clientAccount
            if (clientAccount == EMPTY_ACCOUNT) {
                throw CommandException(translation("liquidbounce.command.marketplace.error.notLoggedIn"))
            }

            val id = args[0] as Int

            withScope {
                MarketplaceApi.deleteMarketplaceItem(clientAccount.takeSession(), id)
                chat(regular(command.result("success", variable(id.toString()))))
            }
        }
        .build()
}
