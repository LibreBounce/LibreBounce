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
package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.doubles.DoubleIterable
import it.unimi.dsi.fastutil.doubles.DoubleIterator
import it.unimi.dsi.fastutil.doubles.DoubleIterators

infix operator fun ClosedRange<Int>.contains(range: ClosedRange<Int>): Boolean {
    return this.start in range && this.endInclusive in range
}

// https://stackoverflow.com/questions/44315977/ranges-in-kotlin-using-data-type-double
infix fun ClosedRange<Double>.step(step: Double): DoubleIterable {
    require(start.isFinite())
    require(endInclusive.isFinite())

    return DoubleIterable {
        if (step == 0.0) {
            DoubleIterators.singleton(this.start)
        } else {
            object : DoubleIterator {
                private var current = start
                private var hasNextValue = current <= endInclusive

                override fun hasNext(): Boolean = hasNextValue

                override fun nextDouble(): Double {
                    if (!hasNextValue) throw NoSuchElementException()
                    val nextValue = current
                    current += step
                    if (current > endInclusive) hasNextValue = false
                    return nextValue
                }

                override fun remove() {
                    throw UnsupportedOperationException("This iterator is read-only")
                }
            }
        }
    }
}

inline fun range(iterable: DoubleIterable, operation: (Double) -> Unit) {
    iterable.doubleIterator().apply {
        while (hasNext()) {
            operation(nextDouble())
        }
    }
}

inline fun range(iterable1: DoubleIterable, iterable2: DoubleIterable, operation: (Double, Double) -> Unit) {
    range(iterable1) { d1 ->
        range(iterable2) { d2 ->
            operation(d1, d2)
        }
    }
}

fun ClosedFloatingPointRange<Float>.random(): Double {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return start + (endInclusive - start) * Math.random()
}

// Due to name conflicts, we have to rename the function
fun ClosedFloatingPointRange<Double>.randomDouble(): Double {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return start + (endInclusive - start) * Math.random()
}

fun ClosedFloatingPointRange<Float>.toDouble(): ClosedFloatingPointRange<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return start.toDouble()..endInclusive.toDouble()
}

fun <T> List<T>.subList(fromIndex: Int): List<T> {
    return this.subList(fromIndex, this.size)
}

/**
 * A JavaScript-styled forEach
 */
inline fun <T, C : Collection<T>> C.forEachWithSelf(action: (T, index: Int, self: C) -> Unit) {
    forEachIndexed { i, item ->
        action(item, i, this)
    }
}
