package example

import example.platform.threads.Task
import example.platform.threads.TaskPool
import example.util.LruCache
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StringTask(val cache: LruCache<Int>, val index: Int) : Task<String>() {

    override fun execute(): String {
        val sharedItem = cache["shared_item"] ?: 0
        val taskIndex = sharedItem + index
        val string = "hello_world_$taskIndex"
        cache["shared_item"] = taskIndex
        return string
    }

}

class AddToCacheTask(val cache: LruCache<Int>, val index: Int) : Task<Int>() {

    override fun execute(): Int {
        cache["shared_item_$index"] = index
        return index
    }

}

class LruCacheTest {

    @Test
    fun testCacheSize1() {
        val cache = LruCache<Int>(10)
        cache["hello"] = 1
        cache["world"] = 2
        assertEquals(2, cache.size)
    }

    @Test
    fun testCacheSize2() {
        val cache = LruCache<Int>(10)
        val taskPool = TaskPool(4)
        for (i in 0..100) {
            taskPool.execute(StringTask(cache, i))
        }
        taskPool.shutdownAndWait()
        assertEquals(1, cache.size)
        assertNotNull(cache["shared_item"])
    }

    @Test
    fun testCacheSize3() {
        val cache = LruCache<Int>(10)
        val taskPool = TaskPool(4)
        for (i in 0..100) {
            taskPool.execute(AddToCacheTask(cache, i))
        }
        taskPool.shutdownAndWait()
        assertEquals(10, cache.size)
    }

}