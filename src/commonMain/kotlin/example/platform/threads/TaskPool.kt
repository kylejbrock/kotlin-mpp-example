package example.platform.threads

interface Task<T> {

    fun execute(): T

}

expect class TaskPool(workerCount: Int) {

    fun <T> execute(task: Task<T>): Result<T>

    fun shutdownAndWait()

}
