/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.config.gson.adapter.lookup

import com.google.gson.*
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.integration.theme.Wallpaper
import java.lang.reflect.Type

/**
 * Unlike a deserializer, this adapter is used to look up a wallpaper by its name.
 * This is useful when the wallpaper cannot be deserialized directly from the JSON and
 * requires an instance to be present in the [ThemeManager.availableWallpapers] map.
 */
object WallpaperLookupAdapter : JsonDeserializer<Wallpaper>, JsonSerializer<Wallpaper> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Wallpaper {
        val jsonObject = json.asJsonObject
        val name = jsonObject["name"].asString
        val theme = jsonObject["theme"].asString

        return ThemeManager.availableWallpapers.find { wallpaper ->
            wallpaper.name == name && wallpaper.theme.name == theme
        } ?: Wallpaper.MinecraftWallpaper
    }

    override fun serialize(
        src: Wallpaper,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ) = JsonObject().apply {
        addProperty("theme", src.theme.name)
        addProperty("name", src.name)
    }

}
