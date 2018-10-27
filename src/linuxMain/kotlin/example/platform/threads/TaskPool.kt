package example.platform.threads

import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.random.Random

actual typealias ResultFuture<T> = Future<T>

actual class TaskPool actual constructor(workerCount: Int) {

    @SharedImmutable
    private val workers = Array(workerCount) { Worker.start() }

    @ThreadLocal
    private val random = Random(4).apply { ensureNeverFrozen() }

    actual fun <T> execute(task: Task<T>): Result<T> {
        task.freeze() // FAIL: Core Dump unless this is commented out.  But, if you comment it out, it fails because of Immutability.  Confused at how to accomplish this.
        val workerIndex = random.nextInt(0, workers.size)
        val worker = workers[workerIndex]
        val future = worker.execute(TransferMode.SAFE, { task }, {
            try {
                val result = it.execute()
                Pair(result, null)
            } catch (e: Throwable) {
                Pair(null, e)
            }
        })
        return Result(future)
    }

    actual fun shutdownAndWait() {
        workers.forEach {
            it.requestTermination(true)
        }
    }

}
