package example.platform.threads

import example.ExampleException

interface Callback<T> {

    fun onResult(value: T?, error: Throwable?)

}

expect class ResultFuture<T> {

    inline fun <R> consume(code: (T) -> R): R

}

class ResultException(message: String, val originalError: Throwable) : ExampleException(message)

class Result<T>(private val future: ResultFuture<Pair<T?, Throwable?>>) {

    private val _complete = AtomicReference(false)
    private val _result = AtomicReference<T?>(null)
    private val _resultError = AtomicReference<Throwable?>(null)
    private val _callbacks = ThreadSafeList<Callback<T>>()
    private val _lock = Lock()

    val value: T?
        get() = _result.value

    val error: Throwable?
        get() = _resultError.value

    val complete: Boolean
        get() = _complete.value

    fun addCallback(callback: Callback<T>) {
        if (complete || _lock.disposed) {
            callback.onResult(value, error)
        } else {
            _lock.sync {
                if (complete) {
                    callback.onResult(value, error)
                } else {
                    _callbacks.add(callback)
                }
            }
        }
    }

    private fun setResult(result: T?, error: Throwable?) {
        if (complete)
            throw IllegalStateException("Result already set")
        _result.set(result)
        _resultError.set(error)
        _complete.set(true)
        // TODO: Core dumps if this is uncommented
//        _callbacks.toList().forEach {
//            it.onResult(result, error)
//        }
        _callbacks.dispose()
    }

    fun getSync(): T? {
        if (complete)
            return value
        return _lock.syncAndDispose {
            if (complete)
                value
            else {
                val result = future.consume { it }
                setResult(result.first, result.second)
                result.second?.let { throw ResultException("Task Exception", it) }
                result.first
            }
        }
    }

}
