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
@file:Suppress("NOTHING_TO_INLINE", "TooManyFunctions")

package net.ccbluex.liquidbounce.utils.math

import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.block.Region
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

inline operator fun BlockPos.rangeTo(other: BlockPos) = Region(this, other)

inline operator fun Vec3i.component1() = this.x
inline operator fun Vec3i.component2() = this.y
inline operator fun Vec3i.component3() = this.z

inline fun BlockPos.copy(x: Int = this.x, y: Int = this.y, z: Int = this.z) = BlockPos(x, y, z)

inline operator fun Vec3i.plus(other: Vec3i): Vec3i {
    return this.add(other)
}

inline operator fun Vec3i.minus(other: Vec3i): Vec3i {
    return this.subtract(other)
}

inline operator fun Vec3i.times(scalar: Int): Vec3i {
    return this.multiply(scalar)
}

inline operator fun Vec3d.plus(other: Vec3d): Vec3d {
    return this.add(other)
}

inline operator fun Vec3d.minus(other: Vec3d): Vec3d {
    return this.subtract(other)
}

inline operator fun Vec3d.times(scalar: Double): Vec3d {
    return this.multiply(scalar)
}

inline fun Vec3d.copy(x: Double = this.x, y: Double = this.y, z: Double = this.z) = Vec3d(x, y, z)

inline operator fun Vec3d.component1(): Double = this.x
inline operator fun Vec3d.component2(): Double = this.y
inline operator fun Vec3d.component3(): Double = this.z

fun Collection<Vec3d>.average(): Vec3d {
    val result = doubleArrayOf(0.0, 0.0, 0.0)
    for (vec in this) {
        result[0] += vec.x
        result[1] += vec.y
        result[2] += vec.z
    }
    return Vec3d(result[0] / size, result[1] / size, result[2] / size)
}

@JvmInline
value class Double3Region private constructor(val init: Array<DoubleArray>) {
    init {
        for (i in 0 until 3) {
            if (init[0][i] > init[1][i]) {
                val temp = init[0][i]
                init[0][i] = init[1][i]
                init[1][i] = temp
            }
        }
    }

    constructor(from: Vec3d, to: Vec3d) : this(
        arrayOf(
            doubleArrayOf(from.x, from.y, from.z),
            doubleArrayOf(to.x, to.y, to.z),
        )
    )

    constructor(from: DoubleArray, to: DoubleArray) : this(arrayOf(from, to)) {
        require(from.size == 3)
        require(to.size == 3)
    }

    infix fun step(step: Double): Sequence<DoubleArray> = sequence {
        val (start, end) = init

        val (startX, startY, startZ) = start
        val (endX, endY, endZ) = end

        var x = startX
        while (x <= endX) {
            var y = startY
            while (y <= endY) {
                var z = startZ
                while (z <= endZ) {
                    yield(doubleArrayOf(x, y, z))
                    z += step
                }
                y += step
            }
            x += step
        }
    }
}

operator fun Vec3d.rangeTo(other: Vec3d) = Double3Region(this, other)

fun Vec3i.toVec3d(): Vec3d = Vec3d.of(this)
fun Vec3i.toVec3d(
    xOffset: Double = 0.0,
    yOffset: Double = 0.0,
    zOffset: Double = 0.0,
): Vec3d = Vec3d(x + xOffset, y + yOffset, z + zOffset)

fun Vec3d.toVec3() = Vec3(this.x, this.y, this.z)
fun Vec3d.toVec3i() = Vec3i(this.x.toInt(), this.y.toInt(), this.z.toInt())

fun Vec3d.toBlockPos() = BlockPos.ofFloored(x, y, z)!!

fun Vec3d.squaredXZDistanceTo(other: Vec3d): Double {
    val d = this.x - other.x
    val e = this.z - other.z
    return d * d + e * e
}

infix fun Vec3d.angleWith(other: Vec3d): Double = this.dotProduct(other) / this.length() / other.length()

val Box.size: Double
    get() = this.lengthX * this.lengthY * this.lengthZ
