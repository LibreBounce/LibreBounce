package net.ccbluex.liquidbounce.utils.clicking

import java.util.*

/**
 * A circular buffer that maintains double the cycle length and regenerates the second half
 * when reaching the midpoint
 */
class RollingClickArray(private val cycleLength: Int) {

    val array = IntArray(cycleLength * 2) // Double the size
    var head = 0
    private val size get() = array.size

    /**
     * Gets value at relative index from current head
     */
    fun get(relativeIndex: Int): Int {
        val actualIndex = (head + relativeIndex) % size
        return array[actualIndex]
    }

    /**
     * Sets value at relative index from current head
     */
    fun set(relativeIndex: Int, value: Int) {
        val actualIndex = (head + relativeIndex) % size
        array[actualIndex] = value
    }

    /**
     * Advances the head position and returns true if halfway point reached
     */
    fun advance(): Boolean {
        head = (head + 1) % size
        return head % cycleLength == 0
    }

    /**
     * Returns list of next cycle values
     */
    fun nextCycleList(): List<Int> {
        return List(cycleLength) { get(it) }
    }

    fun push(cycleArray: IntArray) {
        require(cycleArray.size == cycleLength) { "Array size must match cycle length" }

        if (head == 0) {
            System.arraycopy(cycleArray, 0, array, cycleLength, cycleLength)
        } else if (head == cycleLength) {
            System.arraycopy(cycleArray, 0, array, 0, cycleLength)
        } else {
            throw IllegalStateException("Head must be at 0 or cycle length")
        }
    }

    fun reset() {
        Arrays.fill(array, 0)
        head = 0
    }

}
