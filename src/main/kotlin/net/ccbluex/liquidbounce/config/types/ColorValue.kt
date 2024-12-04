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
package net.ccbluex.liquidbounce.config.types

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import org.apache.commons.lang3.StringUtils
import java.util.*
import kotlin.math.abs
import kotlin.math.min

private const val MAX_SPEED = 100

class ColorValue(name: String, defaultValue: Color4b) :
    Value<Color4b>(name, defaultValue, ValueType.COLOR), CustomDeserializing {

    @SerializedName("rainbowMode")
    var rainbowMode = RainbowMode.NONE

    @SerializedName("rainbowSpeed")
    var rainbowSpeed = 20

    @Exclude
    @ProtocolExclude
    private var temporaryValue: Color4b? = null

    init {
        ColorValueManager.values[this] = null
    }

    override fun get() = temporaryValue ?: super.get()

    override fun deserialize(gson: Gson, jsonObject: JsonObject) {
        println(jsonObject)
        rainbowSpeed = jsonObject["rainbowSpeed"]?.asInt?.coerceIn(0..MAX_SPEED) ?: return
        val modeName = jsonObject["rainbowMode"]?.asString ?: return
        rainbowMode = RainbowMode.entries.firstOrNull {
            StringUtils.equalsIgnoreCase(it.choiceName, modeName)
        } ?: return
        rainbowMode.init(this)
    }

    enum class RainbowMode(override val choiceName: String) : NamedChoice {

        NONE("None") {

            override fun update(value: ColorValue, time: Long) {
                // nothing
            }

            override fun init(value: ColorValue) {
                value.temporaryValue = null
            }

        },
        CYCLE("Cycle") {

            override fun update(value: ColorValue, time: Long) {
                val frequency = getFrequency(value)
                value.temporaryValue = value.inner.hue((time % frequency).toFloat() / frequency)
            }

        },
        PULSE("Pulse") {

            override fun update(value: ColorValue, time: Long) {
                val frequency = getFrequency(value)
                val oscillation = abs((time % (frequency * 2)) - frequency) / frequency
                val brightness = 0.3f - 0.3f * oscillation + min(value.inner.getBrightness(), 0.7f)
                value.temporaryValue = value.inner.brightness(brightness)
            }

        };

        abstract fun update(value: ColorValue, time: Long)

        open fun init(value: ColorValue) {}

        fun getFrequency(value: ColorValue) = ((MAX_SPEED + 1 - value.rainbowSpeed) * 200)

    }

}

object ColorValueManager : EventListener {

    val values = WeakHashMap<ColorValue, Void>()

    @Suppress("unused")
    private val renderHandler = handler<GameRenderEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) {
        val time = System.currentTimeMillis()
        values.keys.forEach { it.rainbowMode.update(it, time) }
    }

}
