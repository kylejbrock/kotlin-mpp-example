package example.platform.threads

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval
import kotlin.native.concurrent.*
import kotlin.random.Random

actual typealias ResultFuture<T> = Future<T>

internal actual class ResultHolder<T> actual constructor(value: T) {

    private val ptr = DetachedObjectGraph { value }.asCPointer()

    actual inline fun <reified T> getValue(): T = DetachedObjectGraph<T>(ptr).attach()

}

private fun seed(): Int {
    memScoped {
        val t: timeval = alloc()
        gettimeofday(t.ptr, null)
        return t.tv_sec.toInt()
    }
}

actual class TaskPool actual constructor(workerCount: Int) {

    @SharedImmutable
    private val workers = Array(workerCount) { Worker.start() }

    @ThreadLocal
    private val random = Random(seed()).apply { ensureNeverFrozen() }

    @SharedImmutable
    private val mutex = Lock()

    actual fun <T> execute(task: Task<T>): Result<T> = mutex.sync {
        if (mutex.disposed)
            throw IllegalStateException("TaskPool is already shutdown")
        val workerIndex = random.nextInt(0, workers.size)
        val worker = workers[workerIndex]
        task.prepare()
        val future = worker.execute(TransferMode.SAFE, { task }, {
            try {
                val result = it.execute()
                Pair(result, null)
            } catch (e: Throwable) {
                e.printStackTrace()
                Pair(null, e)
            }
        })
        Result(future)
    }

    actual fun shutdownAndWait() = mutex.syncAndDispose {
        workers.forEach {
            it.requestTermination(true).result
        }
    }

}
