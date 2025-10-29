package com.example.channel.data

import kotlinx.coroutines.delay
import kotlin.random.Random

class RemoteDataSource(
    private val minDelayMs: Long = 300,
    private val maxDelayMs: Long = 1500
) {
    private val payloadChars = (('a'..'z') + ('0'..'9')).joinToString("")

    suspend fun fetch(url: String): String {
        val wait = Random.nextLong(minDelayMs, maxDelayMs)
        delay(wait)
        val payload = buildString {
            repeat(8) { append(payloadChars.random()) }
        }
        return "url=$url wait=${wait}ms payload=$payload"
    }
}
