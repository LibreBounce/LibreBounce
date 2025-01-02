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
package net.ccbluex.liquidbounce.utils.kotlin

import net.minecraft.util.math.BlockPos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RawBlockPosTest {

    private val blockPos = BlockPos(514, 0, -495)

    @Test
    fun testXYZ() {
        val rawBlockPos = RawBlockPos(blockPos)

        assertEquals(blockPos.x, rawBlockPos.x)
        assertEquals(blockPos.y, rawBlockPos.y)
        assertEquals(blockPos.z, rawBlockPos.z)
    }

    @Test
    fun testCopy() {
        val origin = RawBlockPos(blockPos)
        val copy = origin.copy()

        assertEquals(origin.x, copy.x)
        assertEquals(origin.y, copy.y)
        assertEquals(origin.z, copy.z)
    }

}
