package net.ccbluex.liquidbounce.features.marketplace

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi

data class SubscribedItem(val id: Int, val type: MarketplaceItemType, var installedRevisionId: Int?) {

    /**
     * Check if the item has an update available and install it.
     * This can also be the first-time installation of the item.
     */
    suspend fun update() {
        val updateRevisionId = checkForUpdate() ?: return
        install(updateRevisionId)
    }

    /**
     * Check if the item has an update available.
     *
     * This depends on what item revision is being returned
     * by the Marketplace API as first item. We do not
     * use versioning here, therefore it could also work as downgrade.
     */
    suspend fun checkForUpdate(): Int? {
        val item = MarketplaceApi.getMarketplaceItem(id)

        // If the [item] is not active, we don't want to update it.
        if (item.status != MarketplaceItemStatus.ACTIVE) {
            return null
        }

        // Get the newest revision of the item.
        val revisions = MarketplaceApi.getMarketplaceItemRevisions(id, 1, 1)
        if (revisions.items.isEmpty()) {
            return null
        }

        val newestRevisionId = revisions.items[0].id

        val installedRevisionId = installedRevisionId ?: return newestRevisionId
        return if (installedRevisionId != newestRevisionId) {
            newestRevisionId
        } else {
            null
        }
    }

    suspend fun install(revision: Int) {
        // The revision is already installed, no need to install it again.
        if (revision == installedRevisionId) {
            return
        }



        installedRevisionId = revision
    }


}
