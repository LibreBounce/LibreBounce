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
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.misc.MarketplaceSubscriptionManager
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
                .begin<Int>("page")
                .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                .optional()
                .build()
        )
        .handler { command, args ->
            val page = args.getOrNull(0) as? Int ?: 1

            withScope {
                val response = MarketplaceApi.getMarketplaceItems(page, 10)

                // Filter out pending items
                val activeItems = response.items.filter { it.status != MarketplaceItemStatus.PENDING }

                if (activeItems.isEmpty()) {
                    chat(regular(command.result("noItems")))
                    return@withScope
                }

                chat(regular(command.result("header",
                    variable(page.toString()),
                    variable(response.pagination.pages.toString())
                )))

                for (item in activeItems) {
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
