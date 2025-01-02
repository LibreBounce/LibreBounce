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
package net.ccbluex.liquidbounce.render.engine

import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import org.lwjgl.opengl.GL20
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

data class Vec3(val x: Float, val y: Float, val z: Float) {
    constructor(x: Double, y: Double, z: Double) : this(x.toFloat(), y.toFloat(), z.toFloat())
    constructor(vec: Vec3d) : this(vec.x, vec.y, vec.z)
    constructor(vec: Vec3i) : this(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())

    fun add(other: Vec3): Vec3 {
        return Vec3(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    private fun sub(other: Vec3): Vec3 {
        return Vec3(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    operator fun plus(other: Vec3): Vec3 = add(other)
    operator fun minus(other: Vec3): Vec3 = sub(other)
    operator fun times(scale: Float): Vec3 = Vec3(this.x * scale, this.y * scale, this.z * scale)

    fun rotatePitch(pitch: Float): Vec3 {
        val f = cos(pitch)
        val f1 = sin(pitch)

        val d0 = this.x
        val d1 = this.y * f + this.z * f1
        val d2 = this.z * f - this.y * f1

        return Vec3(d0, d1, d2)
    }

    fun rotateYaw(yaw: Float): Vec3 {
        val f = cos(yaw)
        val f1 = sin(yaw)

        val d0 = this.x * f + this.z * f1
        val d1 = this.y
        val d2 = this.z * f - this.x * f1

        return Vec3(d0, d1, d2)
    }

    fun toVec3d() = Vec3d(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
}

data class UV2f(val u: Float, val v: Float)

/**
 * Directly use primitive int value for Color operations.
 *
 * @author MukjepScarlet
 */
@Suppress("NOTHING_TO_INLINE", "detekt:TooManyFunctions")
@JvmInline
value class Color4b private constructor(val argb: Int) {

    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this((a shl 24) or (r shl 16) or (g shl 8) or b)
    constructor(color: Color) : this(color.rgb)
    constructor(hex: Int, hasAlpha: Boolean = false) : this(if (hasAlpha) hex else 0xFF000000.toInt() or hex)

    inline val r: Int get() = (argb shr 16) and 0xFF
    inline val g: Int get() = (argb shr 8) and 0xFF
    inline val b: Int get() = argb and 0xFF
    inline val a: Int get() = argb ushr 24

    companion object {
        val WHITE = Color4b(0xFFFFFFFF.toInt())
        val BLACK = Color4b(0xFF000000.toInt())
        val RED = Color4b(0xFFFF0000.toInt())
        val GREEN = Color4b(0xFF00FF00.toInt())
        val BLUE = Color4b(0xFF0000FF.toInt())

        @Throws(IllegalArgumentException::class)
        @JvmStatic
        fun fromHex(hex: String): Color4b {
            val cleanHex = hex.removePrefix("#")
            val hasAlpha = cleanHex.length == 8

            require(cleanHex.length == 6 || hasAlpha) { "Invalid hex color string: $hex" }

            return if (hasAlpha) {
                val rgba = cleanHex.toLong(16).toInt()
                Color4b(rgba)
            } else {
                val rgb = cleanHex.toInt(16)
                Color4b(0xFF000000.toInt() or rgb)
            }
        }

        private inline fun darkerChannel(value: Int) = value * 7 / 10
    }

    inline fun with(
        r: Int = this.r,
        g: Int = this.g,
        b: Int = this.b,
        a: Int = this.a
    ): Color4b {
        return Color4b(r, g, b, a)
    }

    inline fun alpha(alpha: Int) = Color4b(hex = this.argb and 0xFFFFFF or (alpha shl 24), hasAlpha = true)

    @Deprecated(message = "Use argb value instead", replaceWith = ReplaceWith("argb"))
    inline fun toARGB() = argb

    inline fun fade(fade: Float): Color4b {
        return if (fade >= 1.0f) {
            this
        } else {
            with(a = (a * fade).toInt())
        }
    }

    fun darker() = Color4b(darkerChannel(r), darkerChannel(g), darkerChannel(b), a)

    inline fun putToUniform(pointer: Int) {
        GL20.glUniform4f(pointer, r / 255f, g / 255f, b / 255f, a / 255f)
    }

}
