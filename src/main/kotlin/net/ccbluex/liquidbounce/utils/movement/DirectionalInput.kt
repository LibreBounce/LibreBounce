package net.ccbluex.liquidbounce.utils.movement

import net.minecraft.client.input.Input

data class DirectionalInput(
    val forwards: Boolean,
    val backwards: Boolean,
    val left: Boolean,
    val right: Boolean,
) {
    constructor(input: Input) : this(input.pressingForward, input.pressingBack, input.pressingLeft, input.pressingRight)

    override fun equals(other: Any?): Boolean {
        return other is DirectionalInput &&
                forwards == other.forwards &&
                backwards == other.backwards &&
                left == other.left &&
                right == other.right
    }

    override fun hashCode(): Int {
        var result = forwards.hashCode()
        result = 31 * result + backwards.hashCode()
        result = 31 * result + left.hashCode()
        result = 31 * result + right.hashCode()
        return result
    }

    val isMoving: Boolean
        get() = forwards || backwards || left || right

    companion object {
        val NONE = DirectionalInput(forwards = false, backwards = false, left = false, right = false)
        val FORWARDS = DirectionalInput(forwards = true, backwards = false, left = false, right = false)
        val BACKWARDS = DirectionalInput(forwards = false, backwards = true, left = false, right = false)
        val LEFT = DirectionalInput(forwards = false, backwards = false, left = true, right = false)
        val RIGHT = DirectionalInput(forwards = false, backwards = false, left = false, right = true)
    }
}
