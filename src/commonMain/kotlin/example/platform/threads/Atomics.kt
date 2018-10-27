package example.platform.threads

expect annotation class SharedImmutable()

expect class AtomicReference<T>(value_: T) {

    var value: T

    fun compareAndSet(expected: T, new: T): Boolean

}

expect fun <T> AtomicReference<T>.set(v: T): Boolean

expect fun Any.ensureNeverFrozen()

expect fun <T> T.freeze(): T