package example.platform.threads

actual annotation class SharedImmutable

actual class AtomicReference<T> actual constructor(value_: T) {

    private val _ref = java.util.concurrent.atomic.AtomicReference<T>(value_)

    actual var value: T
        get() = _ref.get()
        set(value) = _ref.set(value)

    actual fun compareAndSet(expected: T, new: T): Boolean = _ref.compareAndSet(expected, new)

}

actual fun <T> AtomicReference<T>.set(v: T) = compareAndSet(value, v)

actual fun Any.ensureNeverFrozen() {}

actual fun <T> T.freeze(): T {
    return this
}