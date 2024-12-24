package net.ccbluex.liquidbounce.utils.math

/**
 * Finds the minimum between min and max.
 */
inline fun findFunctionMinimumByBisect(
    range: ClosedFloatingPointRange<Double>,
    minDelta: Double = 1E-4,
    function: (Double) -> Double
): Pair<Double, Double> {
    var lowerBound = range.start
    var upperBound = range.endInclusive

    var t = 0

    while (upperBound - lowerBound > minDelta) {
        val mid = (lowerBound + upperBound) / 2

        val leftValue = function((lowerBound + mid) / 2)
        val rightValue = function((mid + upperBound) / 2)

        if (leftValue < rightValue) {
            upperBound = mid
        } else {
            lowerBound = mid
        }

        t++
    }

    val x = (lowerBound + upperBound) / 2
    val y = function(x)

    return x to y
}
