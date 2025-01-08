package net.ccbluex.liquidbounce.features.misc

import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemStatus
import net.ccbluex.liquidbounce.api.models.marketplace.MarketplaceItemType
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.Configurable
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.util.zip.ZipFile

data class SubscribedItem(val id: Int, var latestRevisionId: Int?, val type: MarketplaceItemType)

object MarketplaceSubscriptionManager : Configurable("marketplace") {

    private val subscribedItems by value("subscribed", mutableListOf<SubscribedItem>())
    private val marketplaceRoot = File(ConfigSystem.rootFolder, "marketplace").apply {
        mkdirs()
    }

    fun isSubscribed(itemId: Int) = subscribedItems.any { it.id == itemId }

    suspend fun subscribe(itemId: Int, type: MarketplaceItemType) {
        if (isSubscribed(itemId)) return
        subscribedItems.add(SubscribedItem(itemId, null, type))
        ConfigSystem.storeConfigurable(this)
        try {
            updateItem(itemId)
        } catch (e: Exception) {
            logger.error("Failed to update subscribed item $itemId", e)
            e.printStackTrace()
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

    private suspend fun updateItem(itemId: Int) {
        val item = MarketplaceApi.getMarketplaceItem(itemId)

        if (item.status != MarketplaceItemStatus.ACTIVE) {
            error("Item $itemId is not active")
        }

        val revisions = MarketplaceApi.getMarketplaceItemRevisions(itemId, 1, 1)

        if (revisions.items.isEmpty()) {
            error("Item $itemId has no revisions")
        }

        val latestRevision = revisions.items.filter { revision ->
            revision.status == MarketplaceItemStatus.ACTIVE
        }.maxByOrNull { it.id }!!

        val itemFolder = File(marketplaceRoot, itemId.toString()).apply {
            mkdirs()
        }

        val revisionFolder = File(itemFolder, latestRevision.id.toString()).apply {
            mkdirs()
        }

        val revisionFile = File(revisionFolder, "revision.zip")
        val fileStream: ByteArray = MarketplaceApi.downloadRevision(itemId, latestRevision.id)
        revisionFile.writeBytes(fileStream)

        val dataFolder = when (item.type) {
            MarketplaceItemType.THEME -> ThemeManager.themesFolder
            MarketplaceItemType.SCRIPT -> ScriptManager.root
            else -> error("unsupported")
        }

        ZipFile(revisionFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryDestination = File(dataFolder, entry.name)
                if (entry.isDirectory) {
                    entryDestination.mkdirs()
                } else {
                    entryDestination.outputStream().use { output ->
                        zip.getInputStream(entry).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }

        subscribedItems.find { it.id == itemId }?.latestRevisionId = latestRevision.id
        ConfigSystem.storeConfigurable(this)
    }

}
