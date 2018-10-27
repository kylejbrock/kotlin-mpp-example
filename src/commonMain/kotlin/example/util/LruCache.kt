package example.util

import example.platform.currentTimeInMillis
import example.platform.threads.ThreadSafeMap
import example.platform.threads.ensureNeverFrozen
import example.platform.threads.freeze

private class SharedCacheItem<V>(val key: String, item: V) {

    var lastAccessed: Long = 0

    val item = item
        get() {
            lastAccessed = currentTimeInMillis()
            return field
        }

    init {
        ensureNeverFrozen()
    }

}

private class LruCacheMap<T>(private val maxItems: Int) : ThreadSafeMap<String, SharedCacheItem<T>>() {

    private fun evictLastAccessedItem(backingMap: MutableMap<String, SharedCacheItem<T>>) {
        val items = backingMap.values.sortedBy { -it.lastAccessed }
        backingMap.remove(items.last().key)
    }

    override fun onWillSet(
        backingMap: MutableMap<String, SharedCacheItem<T>>,
        key: String,
        value: SharedCacheItem<T>
    ): SharedCacheItem<T>? {
        if (backingMap.size + 1 > maxItems) {
            evictLastAccessedItem(backingMap)
        }
        value.item // populate last access
        return value
    }

}

class LruCache<T>(maxItems: Int) {

    private val map = LruCacheMap<T>(maxItems)

    init {
        freeze()
    }

    val size: Int
        get() = map.size

    operator fun get(key: String): T? = map.get(key)?.item

    operator fun set(key: String, value: T) = map.set(key, SharedCacheItem(key, value))

    fun remove(key: String): T? = map.remove(key)?.item

}