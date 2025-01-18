/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import net.minecraft.client.gui.FontRenderer
import java.awt.Color

fun int(
    name: String, value: Int, range: IntRange, suffix: String? = null, isSupported: (() -> Boolean)? = null
) = IntValue(name, value, range, suffix).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun float(
    name: String, value: Float, range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE, suffix: String? = null, isSupported: (() -> Boolean)? = null
) = FloatValue(name, value, range, suffix).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun choices(
    name: String, values: Array<String>, value: String, isSupported: (() -> Boolean)? = null
) = ListValue(name, values, value).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun block(
    name: String, value: Int, isSupported: (() -> Boolean)? = null
) = BlockValue(name, value).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun font(
    name: String, value: FontRenderer, isSupported: (() -> Boolean)? = null
) = FontValue(name, value).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun text(
    name: String, value: String, isSupported: (() -> Boolean)? = null
) = TextValue(name, value).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun boolean(
    name: String, value: Boolean, isSupported: (() -> Boolean)? = null
) = BoolValue(name, value).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun intRange(
    name: String, value: IntRange, range: IntRange, suffix: String? = null, isSupported: (() -> Boolean)? = null
) = IntRangeValue(name, value, range, suffix).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun floatRange(
    name: String, value: ClosedFloatingPointRange<Float>, range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
    suffix: String? = null, isSupported: (() -> Boolean)? = null
) = FloatRangeValue(name, value, range, suffix).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun color(
    name: String, value: Color, rainbow: Boolean = false, showPicker: Boolean = false, isSupported: (() -> Boolean)? = null
) = ColorValue(name, value, rainbow, showPicker).apply {
    if (isSupported != null) setSupport { isSupported.invoke() }
}

fun color(
    name: String, value: Int, rainbow: Boolean = false, showPicker: Boolean = false, isSupported: (() -> Boolean)? = null
) = color(name, Color(value, true), rainbow, showPicker, isSupported)
