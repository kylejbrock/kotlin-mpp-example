package example.platform.threads

actual open class ThreadSafeList<V> actual constructor() {

    private val list = mutableListOf<V>()
    private val mutex = Lock()

    actual val size: Int
        get() = mutex.sync {
            list.size
        }

    actual fun add(value: V) = mutex.sync {
        list.add(value)
    }

    actual operator fun get(index: Int): V? = mutex.sync {
        list[index]
    }

    actual fun removeAt(index: Int): V? = mutex.sync {
        list.removeAt(index)
    }

    actual fun dispose() = mutex.syncAndDispose {
        list.clear()
    }

}

actual open class ThreadSafeMap<K, V> actual constructor() {

    private val map = mutableMapOf<K, V>()
    private val mutex = Lock()

    actual val size: Int
        get() = map.size

    actual val keys: Set<K>
        get() = mutex.sync { map.keys.toSet() }

    actual val items: Collection<V>
        get() = mutex.sync { map.values.toList() }

    actual operator fun set(key: K, value: V) = mutex.sync {
        onWillSet(map, key, value)?.let { map[key] = it }
        Unit
    }

    protected actual open fun onWillSet(backingMap: MutableMap<K, V>, key: K, value: V): V? = value

    actual operator fun get(key: K): V? = mutex.sync {
        onWillGet(map, key, map[key])
    }

    protected actual open fun onWillGet(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual fun remove(key: K): V? = mutex.sync {
        onRemoved(map, key, map.remove(key))
    }

    protected actual open fun onRemoved(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual fun dispose() = mutex.syncAndDispose {
        map.clear()
    }

}