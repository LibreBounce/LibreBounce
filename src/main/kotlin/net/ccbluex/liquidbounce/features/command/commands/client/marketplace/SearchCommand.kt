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
