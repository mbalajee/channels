package learn

import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun logTime(tag: String = "", block: suspend () -> Unit) {
    val time = measureTimeMillis {
        runBlocking {
            block()
        }
    }
    println("$tag took ${time}ms")
}
