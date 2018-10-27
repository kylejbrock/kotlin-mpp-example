package example.platform.threads

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.random.Random

actual class ResultFuture<T>(val future: Future<T>) {

    actual inline fun <R> consume(code: (T) -> R): R {
        return code(future.get())
    }

}

actual class TaskPool actual constructor(workerCount: Int) {

    private val workers = Executors.newFixedThreadPool(workerCount)

    private val random = Random(4).apply { ensureNeverFrozen() }

    actual fun <T> execute(task: Task<T>): Result<T> {
        val future = workers.submit(Callable<Pair<T?, Throwable?>> {
            try {
                val result = task.execute()
                Pair(result, null)
            } catch (e: Throwable) {
                Pair(null, e)
            }
        })
        return Result(ResultFuture(future))
    }

    actual fun shutdownAndWait() {
        workers.shutdown()
        workers.awaitTermination(1, TimeUnit.MINUTES)
    }

}
