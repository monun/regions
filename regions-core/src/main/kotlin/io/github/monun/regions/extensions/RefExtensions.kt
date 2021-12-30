package io.github.monun.regions.extensions

import java.lang.ref.WeakReference
import kotlin.reflect.KProperty

fun <T> weak(referent: T) = WeakReference(referent)

operator fun <T> WeakReference<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return get() ?: error("Reference collected by garbage collector.")
}