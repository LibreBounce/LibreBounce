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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * List marketplace items
 */
object ListCommand : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("list")
        .parameter(
            ParameterBuilder
                .begin<String>("type")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ ->
                    MarketplaceItemType.entries.map { it.name.lowercase() }
                        .filter { it.startsWith(begin, ignoreCase = true) }
                }
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder
                .begin<Int>("page")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            val typeStr = args[0] as String
            val page = args.getOrNull(1) as? Int ?: 1

            val type = try {
                MarketplaceItemType.valueOf(typeStr.uppercase())
            } catch (_: IllegalArgumentException) {
                throw CommandException(translation("liquidbounce.command.marketplace.error.invalidItemType"))
            }

            withScope {
                val response = MarketplaceApi.getMarketplaceItems(page, 10, type = type)

                if (response.items.isEmpty()) {
                    chat(regular(command.result("noItems")))
                    return@withScope
                }

                chat(regular(command.result("header",
                    variable(page.toString()),
                    variable(response.pagination.pages.toString())
                )))

                for (item in response.items) {
                    val subscribed = if (MarketplaceManager.isSubscribed(item.id)) "*" else ""
                    chat(
                        regular(
                            command.result(
                                "item",
                                variable(item.id.toString()),
                                variable(item.name),
                                variable(item.type.toString().lowercase()),
                                variable(subscribed)
                            )
                        )
                    )
                }
            }
        }
        .build()
}
