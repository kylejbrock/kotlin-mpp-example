package example.platform.threads

expect class Lock() {

    fun lock()
    fun unlock()
    fun dispose()

    val disposed: Boolean

}

fun <T> Lock.sync(block: () -> T): T {
    if (disposed)
        throw IllegalStateException("Lock is disposed")
    return try {
        lock()
        block()
    } finally {
        unlock()
    }
}

fun <T> Lock.syncAndDispose(block: () -> T): T {
    if (disposed)
        throw IllegalStateException("Lock is disposed")
    return try {
        lock()
        block()
    } finally {
        dispose()
    }
}
