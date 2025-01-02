package net.ccbluex.liquidbounce.utils.kotlin

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

/**
 * Directly use primitive long value for BlockPos operations.
 *
 * @author MukjepScarlet
 */
@Suppress("NOTHING_TO_INLINE", "detekt:TooManyFunctions")
@JvmInline
value class RawBlockPos(val longValue: Long) {

    constructor(blockPos: BlockPos) : this(blockPos.asLong())

    inline val x: Int get() = (longValue shr 38).toInt()
    inline val y: Int get() = (longValue shl 52 shr 52).toInt()
    inline val z: Int get() = (longValue shl 26 shr 38).toInt()

    inline fun move(x: Int = 0, y: Int = 0, z: Int = 0): RawBlockPos {
        return copy(this.x + x, this.y + y, this.z + z)
    }

    inline fun offset(direction: Direction, i: Int = 1): RawBlockPos {
        return move(x = direction.offsetX * i, y = direction.offsetY * i, z = direction.offsetZ * i)
    }

    inline fun down(n: Int = 1): RawBlockPos = move(y = -n)
    inline fun up(n: Int = 1): RawBlockPos = move(y = n)
    inline fun north(n: Int = 1): RawBlockPos = move(z = -n)
    inline fun south(n: Int = 1): RawBlockPos = move(z = n)
    inline fun west(n: Int = 1): RawBlockPos = move(x = -n)
    inline fun east(n: Int = 1): RawBlockPos = move(x = n)

    inline fun copy(x: Int = this.x, y: Int = this.y, z: Int = this.z): RawBlockPos {
        var l = y.toLong() and 0xfff
        l = l or (x.toLong() and 0x3ff_ffff shl 38)
        l = l or (z.toLong() and 0x3ff_ffff shl 12)
        return RawBlockPos(l)
    }

    inline fun asBlockPos(): BlockPos = BlockPos(x, y, z)

    inline fun asMutableBlockPos(): BlockPos.Mutable = BlockPos.Mutable(x, y, z)

    companion object {
        inline fun BlockPos.Mutable.set(raw: RawBlockPos): BlockPos.Mutable = set(raw.longValue)
    }

}
