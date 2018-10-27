package example.platform.threads

import java.util.concurrent.locks.ReentrantLock

actual class Lock actual constructor() {

    private val _lock = ReentrantLock()
    private val _disposed = AtomicReference(false)

    actual val disposed: Boolean
        get() = _disposed.value

    actual fun lock() {
        if (disposed)
            throw IllegalStateException("Already disposed")
        _lock.lock()
    }

    actual fun unlock() {
        if (disposed)
            throw IllegalStateException("Already disposed")
        _lock.unlock()
    }

    actual fun dispose() {
        val alreadyDisposed = _disposed.compareAndSet(false, true)
        if (!alreadyDisposed) {
            if (_lock.isLocked)
                _lock.unlock()
        }
    }

}
