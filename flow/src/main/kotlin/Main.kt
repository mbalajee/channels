package main.kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import learn.DownloadStatus
import learn.Repository
import learn.Downloader

@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runBlocking {
    val repository = Repository.create()
    val maxConcurrent = 10

    val start = System.nanoTime()

    // Producer Flow: emit pending entries marking them Downloading
    val entriesFlow = flow {
        while (true) {
            val next = repository.getNextPendingEntry() ?: break
            repository.setStatus(next, DownloadStatus.Downloading)
            emit(next)
        }
    }

    // Consumer Flow: process downloads with limited concurrency
    entriesFlow
        .flatMapMerge(concurrency = maxConcurrent) { entry ->
            flow {
                val result = runCatching {
                    Downloader.fetch(entry)
                    repository.setStatus(entry, DownloadStatus.Completed)
                    entry
                }
                emit(result)
            }.flowOn(Dispatchers.IO)
        }
        .collect {
            println("Processed: $it")
        }

    val totalMs = (System.nanoTime() - start) / 1_000_000
    println("All downloads complete in ${totalMs}ms")
}
