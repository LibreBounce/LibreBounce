package net.ccbluex.liquidbounce.utils.kotlin

inline fun <T> MutableCollection<T>.removeEach(max: Int = this.size, predicate: (T) -> Boolean) {
    var i = 0
    val iterator = iterator()
    while (iterator.hasNext()) {
        if (i > max) {
            break
        }

        val next = iterator.next()
        if (predicate(next)) {
            iterator.remove()
            i++
        }
    }
}