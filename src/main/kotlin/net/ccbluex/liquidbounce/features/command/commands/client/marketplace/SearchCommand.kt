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
package net.ccbluex.liquidbounce.features.command.commands.client.marketplace

import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.misc.MarketplaceSubscriptionManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

/**
 * Search marketplace items
 */
object SearchCommand : CommandFactory {

    override fun createCommand() = CommandBuilder.begin("search")
        .parameter(
            ParameterBuilder
                .begin<String>("query")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .vararg()
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
            val query = (args[0] as Array<*>).joinToString(" ")
            val page = args.getOrNull(1) as? Int ?: 1

            chat(regular(command.result("searching")))

            withScope {
                val response = MarketplaceApi.getMarketplaceItems(
                    page = page,
                    limit = 10,
                    query = query
                )

                if (response.items.isEmpty()) {
                    chat(regular(command.result("noResults")))
                    return@withScope
                }

                chat(regular(command.result("header",
                    variable(page.toString()),
                    variable(response.pagination.pages.toString())
                )))

                for (item in response.items) {
                    val subscribed = if (MarketplaceSubscriptionManager.isSubscribed(item.id)) "*" else ""
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
