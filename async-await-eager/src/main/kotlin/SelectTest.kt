import kotlinx.coroutines.*

// Demonstration of select { } via the awaitAny() extension defined in Main.kt.
// We create 10 async coroutines with different delays, race them to find the first
// completion, then continue racing the remaining ones until all finish.
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
    // Fixed, predictable delays (ms) so output order is deterministic.
    val delays = listOf(500, 120, 900, 50, 300, 700, 40, 1000, 60, 200)

    // Launch 10 async coroutines; each returns its index after its delay.
    val deferreds = delays.mapIndexed { index, delayMs ->
        async(Dispatchers.Default) {
            delay(delayMs.toLong())
            println("Coroutine #$index finished after ${delayMs}ms")
            index
        }
    }

    println("\nAwaiting first completion using select/awaitAny ...")
    val first = deferreds.awaitAny() // uses select under the hood
    val firstIndex = first.getCompleted()
    println("FIRST winner: coroutine #$firstIndex (delay=${delays[firstIndex]}ms)\n")

    // Consume remaining completions in order they finish.
/*
    val remaining = deferreds.toMutableList()
    remaining.remove(first)
    var rank = 2
    while (remaining.isNotEmpty()) {
        val next = remaining.awaitAny()
        remaining.remove(next)
        val idx = next.getCompleted()
        println("${rank.ordinalLabel()} winner: coroutine #$idx (delay=${delays[idx]}ms)")
        rank++
    }
*/

    println("\nAll coroutines completed.")
}

// Helper to turn an Int rank into a human-friendly ordinal string.
private fun Int.ordinalLabel(): String = when (this) {
    1 -> "1st"
    2 -> "2nd"
    3 -> "3rd"
    else -> this.toString() + "th"
}