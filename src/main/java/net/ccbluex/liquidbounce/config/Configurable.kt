/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.config

import net.minecraft.client.gui.FontRenderer
import java.awt.Color

fun int(
    name: String, value: Int, range: IntRange = 0..Int.MAX_VALUE, suffix: String? = null, subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null
) = IntegerValue(name, value, range, suffix, subjective, isSupported)

fun float(
    name: String, value: Float, range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE, suffix: String? = null,
    subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = FloatValue(name, value, range, suffix, subjective, isSupported)

fun choices(
    name: String, values: Array<String>, value: String, subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null
) = ListValue(name, values, value, subjective, isSupported)

fun block(
    name: String, value: Int, subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = BlockValue(name, value, subjective, isSupported)

fun font(
    name: String, value: FontRenderer, subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = FontValue(name, value, subjective, isSupported)

fun text(
    name: String, value: String, subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = TextValue(name, value, subjective, isSupported)

fun boolean(
    name: String, value: Boolean, subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = BoolValue(name, value, subjective, isSupported)

fun intRange(
    name: String, value: IntRange, range: IntRange = 0..Int.MAX_VALUE, suffix: String? = null,
    subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = IntegerRangeValue(name, value, range, suffix, subjective, isSupported)

fun floatRange(
    name: String, value: ClosedFloatingPointRange<Float>, range: ClosedFloatingPointRange<Float> = 0f..Float.MAX_VALUE,
    suffix: String? = null, subjective: Boolean = false, isSupported: (() -> Boolean)? = null
) = FloatRangeValue(name, value, range, suffix, subjective, isSupported)

fun color(
    name: String, value: Color, rainbow: Boolean = false, showPicker: Boolean = false, subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null
) = ColorValue(name, value, rainbow, showPicker, subjective, isSupported)

fun color(
    name: String, value: Int, rainbow: Boolean = false, showPicker: Boolean = false, subjective: Boolean = false,
    isSupported: (() -> Boolean)? = null
) = color(name, Color(value, true), rainbow, showPicker, subjective, isSupported)
