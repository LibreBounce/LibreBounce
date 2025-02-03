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
import kotlin.Throws
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

data class Color4b(val r: Int, val g: Int, val b: Int, val a: Int = 255) {

    companion object {

        val WHITE = Color4b(255, 255, 255, 255)
        val BLACK = Color4b(0, 0, 0, 255)
        val RED = Color4b(255, 0, 0, 255)
        val GREEN = Color4b(0, 255, 0, 255)
        val BLUE = Color4b(0, 0, 255, 255)
        val CYAN = Color4b(0, 255, 255, 255)
        val MAGENTA = Color4b(255, 0, 255, 255)
        val YELLOW = Color4b(255, 255, 0, 255)
        val ORANGE = Color4b(255, 165, 0, 255)
        val PURPLE = Color4b(128, 0, 128, 255)
        val PINK = Color4b(255, 192, 203, 255)
        val GRAY = Color4b(128, 128, 128, 255)
        val LIGHT_GRAY = Color4b(192, 192, 192, 255)
        val DARK_GRAY = Color4b(64, 64, 64, 255)
        val TRANSPARENT = Color4b(0, 0, 0, 0)

        @Throws(IllegalArgumentException::class)
        fun fromHex(hex: String): Color4b {
            val cleanHex = hex.removePrefix("#")
            val hasAlpha = cleanHex.length == 8

            require(cleanHex.length == 6 || hasAlpha)

            return if (hasAlpha) {
                val rgba = cleanHex.toLong(16)
                Color4b(
                    (rgba shr 24).toInt() and 0xFF,
                    (rgba shr 16).toInt() and 0xFF,
                    (rgba shr 8).toInt() and 0xFF,
                    rgba.toInt() and 0xFF
                )
            } else {
                val rgb = cleanHex.toInt(16)
                Color4b(
                    (rgb shr 16) and 0xFF,
                    (rgb shr 8) and 0xFF,
                    rgb and 0xFF,
                    255
                )
            }
        }

    }

    constructor(color: Color) : this(color.red, color.green, color.blue, color.alpha)
    constructor(hex: Int, hasAlpha: Boolean = false) : this(Color(hex, hasAlpha))

    fun with(
        r: Int = this.r,
        g: Int = this.g,
        b: Int = this.b,
        a: Int = this.a
    ): Color4b {
        return Color4b(r, g, b, a)
    }

    fun alpha(alpha: Int) = Color4b(this.r, this.g, this.b, alpha)

    fun toARGB() = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun toABGR() = (a shl 24) or (b shl 16) or (g shl 8) or r

    fun fade(fade: Float): Color4b {
        return if (fade >= 1.0f) {
            this
        } else {
            with(a = (a * fade).toInt())
        }
    }

    fun darker() = Color4b(darkerChannel(r), darkerChannel(g), darkerChannel(b), a)

    private fun darkerChannel(value: Int) = (value * 0.7).toInt().coerceAtLeast(0)

    fun putToUniform(pointer: Int) {
        GL20.glUniform4f(pointer, r / 255f, g / 255f, b / 255f, a / 255f)
    }

}
