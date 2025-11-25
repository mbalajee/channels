package main.kotlin

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import learn.BatchSize
import learn.DownloadEntry
import learn.DownloadStatus
import learn.Downloader
import learn.Repository

fun main() = runBlocking {
    val repository = Repository.create()

    val start = System.nanoTime()
    val channel = Channel<DownloadEntry>(capacity = BatchSize)

    // Producer: enqueue pending entries
    val producer = launch {
        while (true) {
            val next = repository.getNextPendingEntry() ?: break
            repository.setStatus(next, DownloadStatus.Downloading)

            // Suspends if channel is full
            channel.send(next)
        }
        channel.close()
    }

    // Consumers: process downloads
    val consumers = List(BatchSize) {
        launch(Dispatchers.IO) {
            channel.receive()
            for (entry in channel) {
                try {
                    Downloader.fetch(entry)
                    repository.setStatus(entry, DownloadStatus.Completed)
                } catch (e: Exception) {
                    repository.setStatus(entry, DownloadStatus.Failed)
                }
            }
        }
    }

    producer.join()
    consumers.joinAll()

    val totalMs = (System.nanoTime() - start) / 1_000_000
    println("All downloads complete in ${totalMs}ms")
}
