/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.gson.util.decode
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.minecraft.entity.Entity
import net.minecraft.entity.passive.HorseEntity
import net.minecraft.entity.passive.TameableEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.text.OrderedText
import net.minecraft.text.Style
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * MobOwners module
 *
 * Shows you from which player a tamable entity or projectile belongs to.
 */

object ModuleMobOwners : ClientModule("MobOwners", Category.RENDER) {

    private val projectiles by boolean("Projectiles", false)

    private val uuidNameCache = ConcurrentHashMap<UUID, OrderedText>()

    fun getOwnerInfoText(entity: Entity): OrderedText? {
        if (!this.running) {
            return null
        }

        val ownerId = when {
            entity is TameableEntity -> entity.ownerUuid
            entity is HorseEntity -> entity.ownerUuid
            entity is ProjectileEntity && projectiles -> entity.ownerUuid
            else -> null
        } ?: return null

        return world.getPlayerByUuid(ownerId)
            ?.let { OrderedText.styledForwardsVisitedString(it.nameForScoreboard, Style.EMPTY) }
            ?: getFromMojangApi(ownerId)
    }

    @Suppress("SwallowedException")
    private fun getFromMojangApi(ownerId: UUID): OrderedText {
        return uuidNameCache.computeIfAbsent(ownerId) {
            Util.getDownloadWorkerExecutor().execute {
                try {
                    val uuidAsString = it.toString().replace("-", "")
                    val url = "https://api.mojang.com/user/profiles/$uuidAsString/names"
                    val response = decode<Array<UsernameRecord>>(HttpClient.get(url))

                    val entityName = response.first { it.changedToAt == null }.name

                    uuidNameCache[it] = OrderedText.styledForwardsVisitedString(entityName, Style.EMPTY)
                } catch (_: InterruptedException) {
                } catch (e: Exception) {
                    uuidNameCache[it] = OrderedText.styledForwardsVisitedString(
                        "Failed to query Mojang API",
                        Style.EMPTY.withItalic(true).withColor(Formatting.RED)
                    )
                }
            }

            OrderedText.styledForwardsVisitedString("Loading", Style.EMPTY.withItalic(true))
        }
    }

    private data class UsernameRecord(val name: String, val changedToAt: Int?)

}
