package example.platform.threads

actual fun sleep(millis: Int) = Thread.sleep(millis.toLong())