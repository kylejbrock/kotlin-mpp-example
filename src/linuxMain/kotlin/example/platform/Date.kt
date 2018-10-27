package example.platform

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

actual fun currentTimeInMillis(): Long {
    memScoped {
        val t: timeval = alloc()
        gettimeofday(t.ptr, null)
        return (t.tv_sec * 1000) + (t.tv_usec / 1000)
    }
}