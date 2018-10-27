package example.platform.threads

import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze

actual typealias SharedImmutable = kotlin.native.SharedImmutable

actual typealias AtomicReference<T> = kotlin.native.concurrent.AtomicReference<T>

actual fun <T> AtomicReference<T>.set(v: T) = compareAndSet(value, v.freeze())

actual fun Any.ensureNeverFrozen() = this.ensureNeverFrozen()

actual fun <T> T.freeze() = this.freeze()