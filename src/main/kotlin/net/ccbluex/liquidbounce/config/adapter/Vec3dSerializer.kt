/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 202 CCBlueX
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
package net.ccbluex.liquidbounce.config.adapter

import com.google.gson.*
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.lang.reflect.Type

/**
 * Please use [Vec3i] instead of [BlockPos] for serialization.
 */
object Vec3iSerializer : JsonSerializer<Vec3i>, JsonDeserializer<Vec3i> {

    override fun serialize(src: Vec3i, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("x", src.x)
        addProperty("y", src.y)
        addProperty("z", src.z)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) = Vec3i(
        json.asJsonObject["x"].asInt,
        json.asJsonObject["y"].asInt,
        json.asJsonObject["z"].asInt
    )

}

object Vec3dSerializer : JsonSerializer<Vec3d>, JsonDeserializer<Vec3d> {

    override fun serialize(src: Vec3d, typeOfSrc: Type, context: JsonSerializationContext) = JsonObject().apply {
        addProperty("x", src.x)
        addProperty("y", src.y)
        addProperty("z", src.z)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?) = Vec3d(
        json.asJsonObject["x"].asDouble,
        json.asJsonObject["y"].asDouble,
        json.asJsonObject["z"].asDouble
    )

}
