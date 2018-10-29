package example.platform.threads

expect open class ThreadSafeList<V>() {

    val size: Int

    fun add(value: V): Boolean

    operator fun get(index: Int): V?

    fun removeAt(index: Int): V?

    fun dispose()

}

expect open class ThreadSafeMap<K, V>() {

    val size: Int

    val keys: Set<K>

    val items: Collection<V>

    operator fun set(key: K, value: V)

    protected open fun onWillSet(backingMap: MutableMap<K, V>, key: K, value: V?): V?

    operator fun get(key: K): V?

    protected open fun onWillGet(backingMap: MutableMap<K, V>, key: K, value: V?): V?

    fun remove(key: K): V?

    protected open fun onRemoved(backingMap: MutableMap<K, V>, key: K, value: V?): V?

    fun dispose()

}
