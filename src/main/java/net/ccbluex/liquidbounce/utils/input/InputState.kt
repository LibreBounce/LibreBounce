package net.ccbluex.liquidbounce.utils.input

// Credits to Illusion Client for this!
class InputState(
	var forward: Boolean
	var back: Boolean
	var left: Boolean
	var right: Boolean
	var jump: Boolean
	var sneak: Boolean
) {

	fun forwardAxis(): Float
		get() = axis(forward, back)

	fun sidewaysAxis(): Float
		get() = axis(left, right)

	fun copy(InputState other) {
		forward = other.forward
		back = other.back
		left = other.left
		right = other.right
		jump = other.jump
		sneak = other.sneak
	}

	private fun axis(positive: Boolean, negative: Boolean): Float
		get() = if (positive) 1.0f else 0.0f - if (negative) 1.0f else 0.0f
}