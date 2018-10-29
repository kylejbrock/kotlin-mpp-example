package example.platform.threads

abstract class Task<T> {

    fun prepare() {
        freeze()
    }

    abstract fun execute(): T

}

expect class TaskPool(workerCount: Int) {

    fun <T> execute(task: Task<T>): Result<T>

    fun shutdownAndWait()

}
