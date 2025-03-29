package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

data class SubscribedItem(
    val id: Int,
    val type: MarketplaceItemType,

    var latestRevisionId: Int?,
    @Exclude
    var updateCheckTime: Long = 0L,
)

object MarketplaceManager : Configurable("marketplace"), EventListener {

    private val subscribedItems by value("subscribed", mutableListOf<SubscribedItem>())
    private val marketplaceRoot = File(ConfigSystem.rootFolder, "marketplace").apply {
        mkdirs()
    }

    fun isSubscribed(itemId: Int) = subscribedItems.any { it.id == itemId }

    suspend fun subscribe(itemId: Int, type: MarketplaceItemType) {
        if (isSubscribed(itemId)) return
        subscribedItems.add(SubscribedItem(itemId, type, null))
        ConfigSystem.storeConfigurable(this)

        try {
            updateItem(itemId)
        } catch (e: Exception) {
            logger.error("Failed to update subscribed item $itemId", e)
        }
    }

    suspend fun unsubscribe(itemId: Int) {
        subscribedItems.removeIf { it.id == itemId }
        ConfigSystem.storeConfigurable(this)
        val itemFolder = File(marketplaceRoot, itemId.toString())
        if (itemFolder.exists()) {
            itemFolder.deleteRecursively()
        }
        ConfigSystem.storeConfigurable(this)
    }

    suspend fun updateAllSubscriptions() {
        subscribedItems.forEach { item ->
            updateItem(item.id)
        }
    }

    @Suppress("unused")
    private val gameTickHandler = tickHandler {
        // Check if recent
    }

    internal suspend fun updateItem(itemId: Int) {
        val item = MarketplaceApi.getMarketplaceItem(itemId)

        if (item.status != MarketplaceItemStatus.ACTIVE) {
            error("Item $itemId is not active")
        }

        val revisions = MarketplaceApi.getMarketplaceItemRevisions(itemId, 1, 1)

        if (revisions.items.isEmpty()) {
            error("Item $itemId has no revisions")
        }

        TODO("Implement installation of item")
        ConfigSystem.storeConfigurable(this)
    }

}
