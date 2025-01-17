/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.kotlin.coerceIn
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.minecraft.client.gui.FontRenderer
import org.lwjgl.input.Mouse
import java.awt.Color
import javax.vecmath.Vector2f
import kotlin.math.roundToInt
import kotlin.reflect.KProperty

/**
 * Bool value represents a value with a boolean
 */
open class BoolValue(
    name: String,
    value: Boolean,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<Boolean>(name, value, subjective, isSupported) {

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) =
        if (element.isJsonPrimitive) element.asBoolean || element.asString.equals("true", ignoreCase = true)
        else null

    fun toggle() = set(!value)

    fun isActive() = value && (isSupported() || hidden)

    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return super.getValue(thisRef, property) && isActive()
    }
}

/**
 * Integer value represents a value with a integer
 */
open class IntegerValue(
    name: String,
    value: Int,
    val range: IntRange = 0..Int.MAX_VALUE,
    suffix: String? = null,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<Int>(name, value, subjective, isSupported, suffix) {

    override fun validate(newValue: Int): Int = newValue.coerceIn(range)

    fun set(newValue: Number) = set(newValue.toInt())

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asInt else null

    fun isMinimal() = value <= minimum
    fun isMaximal() = value >= maximum

    val minimum = range.first
    val maximum = range.last
}

// TODO: Replace Min/Max options with this instead
class IntegerRangeValue(
    name: String,
    value: IntRange,
    val range: IntRange = 0..Int.MAX_VALUE,
    suffix: String? = null,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<IntRange>(name, value, subjective, isSupported, suffix) {

    override fun validate(newValue: IntRange): IntRange = newValue.coerceIn(range)

    fun setFirst(newValue: Int, immediate: Boolean = true) = set(newValue..value.last, immediate)
    fun setLast(newValue: Int, immediate: Boolean = true) = set(value.first..newValue, immediate)

    override fun toJsonF(): JsonElement {
        return JsonPrimitive("${value.first}-${value.last}")
    }

    override fun fromJsonF(element: JsonElement): IntRange? {
        return element.asJsonPrimitive?.asString?.split("-")?.takeIf { it.size == 2 }?.let {
            val (start, end) = it

            start.toIntOrNull()?.let { s ->
                end.toIntOrNull()?.let { e ->
                    s..e
                }
            }
        }
    }

    fun isMinimal() = value.first <= minimum
    fun isMaximal() = value.last >= maximum

    val minimum = range.first
    val maximum = range.last

    val random
        get() = nextInt(value.first, value.last)
}

/**
 * Float value represents a value with a float
 */
open class FloatValue(
    name: String,
    value: Float,
    val range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
    suffix: String? = null,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<Float>(name, value, subjective, isSupported, suffix) {

    override fun validate(newValue: Float): Float = newValue.coerceIn(range)

    fun set(newValue: Number) = set(newValue.toFloat())

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asFloat else null

    fun isMinimal() = value <= minimum
    fun isMaximal() = value >= maximum

    val minimum = range.start
    val maximum = range.endInclusive
}

// TODO: Replace Min/Max options with this instead
class FloatRangeValue(
    name: String,
    value: ClosedFloatingPointRange<Float>,
    val range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
    suffix: String? = null,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<ClosedFloatingPointRange<Float>>(name, value, subjective, isSupported, suffix) {

    override fun validate(newValue: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float> = newValue.coerceIn(range)

    fun setFirst(newValue: Float, immediate: Boolean = true) = set(newValue..value.endInclusive, immediate)
    fun setLast(newValue: Float, immediate: Boolean = true) = set(value.start..newValue, immediate)

    override fun toJsonF(): JsonElement {
        return JsonPrimitive("${value.start}-${value.endInclusive}")
    }

    override fun fromJsonF(element: JsonElement): ClosedFloatingPointRange<Float>? {
        return element.asJsonPrimitive?.asString?.split("-")?.takeIf { it.size == 2 }?.let {
            val (start, end) = it

            start.toFloatOrNull()?.let { s ->
                end.toFloatOrNull()?.let { e ->
                    s..e
                }
            }
        }
    }

    fun isMinimal() = value.start <= minimum
    fun isMaximal() = value.endInclusive >= maximum

    val minimum = range.start
    val maximum = range.endInclusive

    val random
        get() = nextFloat(value.start, value.endInclusive)
}

/**
 * Text value represents a value with a string
 */
open class TextValue(
    name: String,
    value: String,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<String>(name, value, subjective, isSupported) {

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asString else null
}

/**
 * Font value represents a value with a font
 */
open class FontValue(
    name: String,
    value: FontRenderer,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<FontRenderer>(name, value, subjective, isSupported) {

    override fun toJsonF(): JsonElement? {
        val fontDetails = Fonts.getFontDetails(value) ?: return null
        val valueObject = JsonObject()
        valueObject.run {
            addProperty("fontName", fontDetails.name)
            addProperty("fontSize", fontDetails.size)
        }
        return valueObject
    }

    override fun fromJsonF(element: JsonElement) = if (element.isJsonObject) {
        val valueObject = element.asJsonObject
        Fonts.getFontRenderer(valueObject["fontName"].asString, valueObject["fontSize"].asInt)
    } else null

    val displayName
        get() = when (value) {
            is GameFontRenderer -> "Font: ${(value as GameFontRenderer).defaultFont.font.name} - ${(value as GameFontRenderer).defaultFont.font.size}"
            Fonts.minecraftFont -> "Font: Minecraft"
            else -> {
                val fontInfo = Fonts.getFontDetails(value)
                fontInfo?.let {
                    "${it.name}${if (it.size != -1) " - ${it.size}" else ""}"
                } ?: "Font: Unknown"
            }
        }

    fun next() {
        val fonts = Fonts.fonts
        value = fonts[(fonts.indexOf(value) + 1) % fonts.size]
    }

    fun previous() {
        val fonts = Fonts.fonts
        value = fonts[(fonts.indexOf(value) - 1 + fonts.size) % fonts.size]
    }
}

/**
 * Block value represents a value with a block
 */
open class BlockValue(
    name: String, value: Int, subjective: Boolean = false, isSupported: (() -> Boolean)? = null,
) : IntegerValue(name, value, 1..197, null, subjective, isSupported)

/**
 * List value represents a selectable list of values
 */
open class ListValue(
    name: String,
    var values: Array<String>,
    override var value: String,
    subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null,
) : Value<String>(name, value, subjective, isSupported) {

    override fun validate(newValue: String): String = values.find { it.equals(newValue, true) } ?: default

    var openList = false

    operator fun contains(string: String?) = values.any { it.equals(string, true) }

    override fun changeValue(newValue: String) {
        values.find { it.equals(newValue, true) }?.let { value = it }
    }

    override fun toJsonF() = JsonPrimitive(value)

    override fun fromJsonF(element: JsonElement) = if (element.isJsonPrimitive) element.asString else null

    fun updateValues(newValues: Array<String>) {
        values = newValues
    }
}

class ColorValue(
    name: String, defaultColor: Color, var rainbow: Boolean = false, var showPicker: Boolean = false,
    subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) : Value<Color>(name, defaultColor, subjective = subjective, isSupported = isSupported) {
    // Sliders
    var hueSliderY = 0F
    var opacitySliderY = 0F

    // Slider positions in the 0-1 range
    var colorPickerPos = Vector2f(0f, 0f)

    var lastChosenSlider: SliderType? = null
        get() {
            if (!Mouse.isButtonDown(0)) field = null
            return field
        }

    init {
        setupSliders(defaultColor)
    }

    fun setupSliders(color: Color) {
        Color.RGBtoHSB(color.red, color.green, color.blue, null).also {
            hueSliderY = it[0]
            opacitySliderY = color.alpha / 255f
            colorPickerPos.set(it[1], 1 - it[2])
        }
    }

    fun selectedColor() = if (rainbow) {
        ColorUtils.rainbow(alpha = opacitySliderY)
    } else {
        get()
    }

    override fun toJsonF(): JsonElement {
        val pos = colorPickerPos
        return JsonPrimitive("colorpicker: [${pos.x}, ${pos.y}], hueslider: ${hueSliderY}, opacity: ${opacitySliderY}, rainbow: $rainbow")
    }

    override fun fromJsonF(element: JsonElement): Color? {
        if (element.isJsonPrimitive) {
            val raw = element.asString

            val regex =
                """colorpicker:\s*\[\s*(-?\d*\.?\d+),\s*(-?\d*\.?\d+)\s*],\s*hueslider:\s*(-?\d*\.?\d+),\s*opacity:\s*(-?\d*\.?\d+),\s*rainbow:\s*(true|false)""".toRegex()
            val matchResult = regex.find(raw)

            if (matchResult != null) {
                val colorPickerX = matchResult.groupValues[1].toFloatOrNull()
                val colorPickerY = matchResult.groupValues[2].toFloatOrNull()
                val hueSliderY = matchResult.groupValues[3].toFloatOrNull()
                val opacitySliderY = matchResult.groupValues[4].toFloatOrNull()
                val rainbowString = matchResult.groupValues[5].toBoolean()

                if (colorPickerX != null && colorPickerY != null && hueSliderY != null && opacitySliderY != null) {
                    colorPickerPos = Vector2f(colorPickerX, colorPickerY)
                    this.hueSliderY = hueSliderY
                    this.opacitySliderY = opacitySliderY
                    this.rainbow = rainbowString

                    // Change the current color based on the data from values.json
                    return Color(
                        Color.HSBtoRGB(this.hueSliderY, colorPickerX, 1 - colorPickerY), true
                    ).withAlpha((opacitySliderY * 255).roundToInt())
                }
            }
        }
        return null
    }

    override fun getString() =
        "Color[picker=[${colorPickerPos.x},${colorPickerPos.y}],hueslider=${hueSliderY},opacity=${(opacitySliderY)},rainbow=$rainbow]"

    override fun getValue(thisRef: Any?, property: KProperty<*>): Color {
        return selectedColor()
    }

    // Every change that is not coming from any ClickGUI styles should modify the sliders to synchronize with the new color.
    init {
        onChanged(::setupSliders)
    }

    fun readColorFromConfig(str: String): List<String>? {
        val regex =
            """Color\[picker=\[\s*(-?\d*\.?\d+),\s*(-?\d*\.?\d+)],\s*hueslider=\s*(-?\d*\.?\d+),\s*opacity=\s*(-?\d*\.?\d+),\s*rainbow=(true|false)]""".toRegex()
        val matchResult = regex.find(str)

        return matchResult?.groupValues?.drop(1)
    }

    enum class SliderType {
        COLOR, HUE, OPACITY
    }
}