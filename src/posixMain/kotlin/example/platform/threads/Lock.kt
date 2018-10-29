package example.platform.threads

import kotlinx.cinterop.Arena
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.*
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.attach

internal class LockHandle {

    private val arena = Arena()
    val lock: pthread_mutex_t

    init {
        ensureNeverFrozen()
        lock = arena.alloc()
        if (pthread_mutex_init(lock.ptr, null) != 0) {
            throw IllegalStateException("Failed to initialize mutex")
        }
    }

    fun dispose() {
        pthread_mutex_destroy(lock.ptr)
        arena.clear()
    }

}

actual class Lock actual constructor() {

    private val _handle = DetachedObjectGraph { LockHandle() }.asCPointer()
    private val _disposed = AtomicReference(false)
    private val _locked = AtomicReference(false)

    init {
        freeze()
    }

    actual val disposed: Boolean
        get() = _disposed.value

    private val attachedHandle: LockHandle
        get() = DetachedObjectGraph<LockHandle>(_handle).attach()

    actual fun lock() {
        if (disposed)
            throw IllegalStateException("Already disposed")
        pthread_mutex_lock(attachedHandle.lock.ptr)
        _locked.set(true)
    }

    actual fun unlock() {
        if (disposed)
            throw IllegalStateException("Already disposed")
        _locked.set(false)
        pthread_mutex_unlock(attachedHandle.lock.ptr)
    }

    actual fun dispose() {
        val alreadyDisposed = _disposed.compareAndSet(false, true)
        if (!alreadyDisposed) {
            val handle = attachedHandle
            if (_locked.value)
                pthread_mutex_unlock(handle.lock.ptr)
            handle.dispose()
        }
    }

}
