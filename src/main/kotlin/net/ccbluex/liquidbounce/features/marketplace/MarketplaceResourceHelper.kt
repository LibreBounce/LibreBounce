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
package net.ccbluex.liquidbounce.features.marketplace

import net.ccbluex.liquidbounce.LiquidBounce.logger
import net.ccbluex.liquidbounce.api.services.marketplace.MarketplaceApi
import net.ccbluex.liquidbounce.utils.io.extractZip

object MarketplaceResourceHelper {

    suspend fun download(itemId: Int, revisionId: Int) {
        MarketplaceApi.downloadRevision(itemId, revisionId).use { inputStream ->
            val itemRoot = MarketplaceManager.marketplaceRoot.resolve("items/$itemId")
            if (!itemRoot.exists()) {
                itemRoot.mkdirs()
            }

            val revisionRoot = MarketplaceManager.marketplaceRoot.resolve("revisions/$revisionId")
            if (!revisionRoot.exists()) {
                revisionRoot.mkdirs()
            }

            extractZip(inputStream.inputStream(), revisionRoot)
            logger.debug("Downloaded item {} revision {} to {}", itemId, revisionId, revisionRoot)
        }
    }

}
