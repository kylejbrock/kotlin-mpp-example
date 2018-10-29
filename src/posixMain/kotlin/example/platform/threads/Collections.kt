package example.platform.threads

import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.attach

actual open class ThreadSafeList<V> actual constructor() {

    private val backingList = AtomicReference(DetachedObjectGraph {
        val list = mutableListOf<V>()
        list.ensureNeverFrozen()
        list
    }.asCPointer()!!)
    private val mutex = Lock()

    private inline fun <T> execute(crossinline run: (list: MutableList<V>) -> T) = mutex.sync {
        var result: T? = null
        val ptr = DetachedObjectGraph(TransferMode.UNSAFE) {
            val list = DetachedObjectGraph<MutableList<V>>(backingList.value).attach()
            result = run(list)
            list
        }.asCPointer() ?: throw IllegalStateException("Failed to get new pointer")
        if (!backingList.set(ptr)) {
            throw IllegalStateException()
        }
        result
    }

    private inline fun <T> executeAndDispose(crossinline run: (map: MutableList<V>) -> T) = mutex.syncAndDispose {
        var result: T? = null
        val ptr = DetachedObjectGraph(TransferMode.UNSAFE) {
            val list = DetachedObjectGraph<MutableList<V>>(backingList.value).attach()
            result = run(list)
            list
        }.asCPointer() ?: throw IllegalStateException("Failed to get new pointer")
        if (!backingList.set(ptr)) {
            throw IllegalStateException()
        }
        result!!
    }

    actual val size: Int
        get() = execute {
            it.size
        }!!

    actual fun add(value: V) = execute {
        it.add(value)
    }!!

    actual operator fun get(index: Int): V? = execute {
        it[index]
    }

    actual fun removeAt(index: Int): V? = execute {
        it.removeAt(index)
    }

    actual fun dispose() = executeAndDispose {
        it.clear()
    }

}

actual open class ThreadSafeMap<K, V> actual constructor() {

    private var backingMap = AtomicReference(DetachedObjectGraph {
        val map = mutableMapOf<K, V>()
        map.ensureNeverFrozen()
        map
    }.asCPointer()!!)
    private val mutex = Lock()

    private inline fun <T> execute(crossinline run: (map: MutableMap<K, V>) -> T) = mutex.sync {
        var result: T? = null
        val ptr = DetachedObjectGraph(TransferMode.UNSAFE) {
            val map = DetachedObjectGraph<MutableMap<K, V>>(backingMap.value).attach()
            result = run(map)
            map
        }.asCPointer() ?: throw IllegalStateException("Failed to get new pointer")
        if (!backingMap.set(ptr)) {
            throw IllegalStateException()
        }
        result
    }

    private inline fun <T> executeAndDispose(crossinline run: (map: MutableMap<K, V>) -> T) = mutex.syncAndDispose {
        var result: T? = null
        val ptr = DetachedObjectGraph(TransferMode.UNSAFE) {
            val map = DetachedObjectGraph<MutableMap<K, V>>(backingMap.value).attach()
            result = run(map)
            map
        }.asCPointer() ?: throw IllegalStateException("Failed to get new pointer")
        if (!backingMap.set(ptr)) {
            throw IllegalStateException()
        }
        result
    }

    actual val size: Int
        get() = execute { it.size }!!

    actual val keys: Set<K>
        get() = execute { it.keys.toSet() }!!

    actual val items: Collection<V>
        get() = execute { it.values.toList() }!!

    actual operator fun set(key: K, value: V) = execute { backingMap ->
        val newValue = onWillSet(backingMap, key, value)
        if (newValue != null)
            backingMap[key] = newValue
        else
            onRemoved(backingMap, key, backingMap.remove(key))
        Unit
    }!!

    protected actual open fun onWillSet(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual operator fun get(key: K): V? = execute { backingMap ->
        onWillGet(backingMap, key, backingMap[key])
    }

    protected actual open fun onWillGet(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual fun remove(key: K): V? = execute { backingMap ->
        onRemoved(backingMap, key, backingMap.remove(key))
    }

    protected actual open fun onRemoved(backingMap: MutableMap<K, V>, key: K, value: V?): V? = value

    actual fun dispose() = executeAndDispose {
        it.clear()
    }!!

}