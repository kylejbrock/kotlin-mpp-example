package example.platform.threads

import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach

actual open class ThreadSafeList<V> actual constructor() {

    private val backingList = DetachedObjectGraph {
        val list = mutableListOf<V>()
        list.ensureNeverFrozen()
        list
    }.asCPointer()
    private val mutex = Lock()

    private val attachedList: MutableList<V>
        get() = DetachedObjectGraph<MutableList<V>>(backingList).attach()

    actual val size: Int
        get() = mutex.sync {
            attachedList.size
        }

    actual fun add(value: V) = mutex.sync {
        attachedList.add(value)
    }

    actual operator fun get(index: Int): V? = mutex.sync {
        attachedList[index]
    }

    actual fun removeAt(index: Int): V? = mutex.sync {
        attachedList.removeAt(index)
    }

    actual fun dispose() = mutex.syncAndDispose {
        attachedList.clear()
    }

}

actual open class ThreadSafeMap<K, V> actual constructor() {

    private val backingMap = DetachedObjectGraph {
        val map = mutableMapOf<K, V>()
        map.ensureNeverFrozen()
        map
    }.asCPointer()
    private val mutex = Lock()

    private val attachedMap: MutableMap<K, V>
        get() = DetachedObjectGraph<MutableMap<K, V>>(backingMap).attach()

    actual val size: Int
        get() = mutex.sync { attachedMap.size }

    actual val keys: Set<K>
        get() = mutex.sync { attachedMap.keys.toSet() }

    actual val items: Collection<V>
        get() = mutex.sync { attachedMap.values.toList() }

    actual operator fun set(key: K, value: V) = mutex.sync {
        val backingMap = attachedMap
        onWillSet(backingMap, key, value)?.let { backingMap[key] = it }
        Unit
    }

    protected actual open fun onWillSet(backingMap: MutableMap<K, V>, key: K, value: V): V? = value

    actual operator fun get(key: K): V? = mutex.sync {
        val backingMap = attachedMap
        onWillGet(backingMap, key, backingMap[key])
    }

    protected actual open fun onWillGet(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual fun remove(key: K): V? = mutex.sync {
        val backingMap = attachedMap
        onRemoved(backingMap, key, backingMap.remove(key))
    }

    protected actual open fun onRemoved(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual fun dispose() = mutex.syncAndDispose {
        attachedMap.clear()
    }

}