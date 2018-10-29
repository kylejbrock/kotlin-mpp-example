package example

import example.platform.threads.ThreadSafeMap
import example.platform.threads.ensureNeverFrozen
import example.platform.threads.freeze
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlin.collections.set
import kotlin.native.concurrent.DetachedObjectGraph
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.attach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

data class MutableObject(var i: Int) {
    init {
        ensureNeverFrozen()
    }
}

class DetachedObjectGraphTest {

    private fun mutationTest1(test: COpaquePointer): COpaquePointer {
        return DetachedObjectGraph {
            val map = DetachedObjectGraph<MutableMap<String, MutableObject>>(test).attach()
            map["test1"] = MutableObject(2)
            map
        }.asCPointer()!!
    }

    private fun mutationTest2(test: COpaquePointer): COpaquePointer {
        return DetachedObjectGraph {
            val map = DetachedObjectGraph<MutableMap<String, MutableObject>>(test).attach()
            assertEquals(1, map.size)
            map["test1"]!!.i += 2
            map
        }.asCPointer()!!
    }

    private fun mutationTest3(test: COpaquePointer): COpaquePointer {
        return DetachedObjectGraph {
            val map = DetachedObjectGraph<MutableMap<String, MutableObject>>(test).attach()
            map["test1"]!!.i += 2
            map
        }.asCPointer()!!
    }

    private fun mutationTest4(test: COpaquePointer): COpaquePointer {
        return DetachedObjectGraph {
            val map = DetachedObjectGraph<MutableMap<String, MutableObject>>(test).attach()
            assertEquals(6, map["test1"]!!.i)
            map
        }.asCPointer()!!
    }

    @Test
    fun testDetachedObjectGraph() {
        var ptr = DetachedObjectGraph {
            val map = mutableMapOf<String, MutableObject>()
            map.ensureNeverFrozen()
            map
        }.asCPointer()
        assertNotNull(ptr)
        ptr = mutationTest1(ptr!!)
        ptr = mutationTest2(ptr)
        ptr = mutationTest3(ptr)
        mutationTest4(ptr)
    }

    @Test
    fun testNativeHandleAndDetachedObjectGraph() {
        memScoped {
            val nativeHandle = alloc<NativeHandle>() // see nativeInterop/cinterop/global.def
            nativeHandle.handle = DetachedObjectGraph {
                val map = mutableMapOf<String, MutableObject>()
                map.ensureNeverFrozen()
                map
            }.asCPointer()
            assertNotNull(nativeHandle.handle)
            nativeHandle.handle = mutationTest1(nativeHandle.handle!!)
            nativeHandle.handle = mutationTest2(nativeHandle.handle!!)
            nativeHandle.handle = mutationTest3(nativeHandle.handle!!)
            nativeHandle.handle = mutationTest4(nativeHandle.handle!!)
        }
    }

    @Test
    fun testMap() {
        val map = ThreadSafeMap<String, Int>().freeze()
        map["hello"] = 1
        assertEquals(1, map.size)

        val worker = Worker.start()
        worker.execute(TransferMode.SAFE, { map }, {
            it["world"] = 2
        })
        worker.requestTermination(true).result

        assertEquals(2, map.size)
        assertEquals(1, map["hello"])
        assertEquals(2, map["world"])

        map["world"] = 3
        assertEquals(2, map.size)
        assertEquals(3, map["world"])

        val worker2 = Worker.start()
        worker2.execute(TransferMode.SAFE, { map }, {
            assertEquals(2, it.size)
            assertEquals(3, it["world"])
            it["world"] = 30
        })
        worker2.requestTermination(true).result
        assertEquals(2, map.size)
        assertEquals(1, map["hello"])
        assertEquals(30, map["world"])
    }

    @Test
    fun testMap2() {
        val map = ThreadSafeMap<String, MutableObject>().freeze()
        map["hello"] = MutableObject(2)
        assertEquals(1, map.size)

        val worker = Worker.start()
        worker.execute(TransferMode.SAFE, { map }, {
            it["hello"]!!.i = 3
        })
        worker.requestTermination(true).result

        assertEquals(1, map.size)
        assertEquals(3, map["hello"]!!.i)
    }

}