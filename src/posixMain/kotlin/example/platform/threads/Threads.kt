package example.platform.threads

import kotlinx.cinterop.convert
import platform.posix.usleep

actual fun sleep(millis: Int) {
    usleep((millis * 1000).convert())
}