package net.ccbluex.liquidbounce.utils.kotlin

/**
 * @param maxSize Maximum size of the cache. The best values are 2 to the power of [Int] minus one like 63, 127, 255...
 */
class LruCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize + 1, 1f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        return size > maxSize
    }
}
