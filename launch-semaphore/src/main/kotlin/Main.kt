package main.kotlin

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import learn.BatchSize
import learn.DownloadStatus
import learn.Downloader
import learn.Repository
import learn.logTime

fun main() = runBlocking {
    val repository = Repository.create()
    val semaphore = Semaphore(BatchSize)

    logTime {
        supervisorScope {
            repeat(BatchSize) {
                launch {
                    download(repository, semaphore)
                }
            }
        }
    }
}

// Worker coroutine representing one concurrency slot.
private suspend fun download(
    repository: Repository,
    semaphore: Semaphore
) {
    while (true) {
        val next = repository.getNextPendingEntry() ?: break // queue drained

        // Acquire permit before marking as downloading to reflect active concurrency.
        semaphore.acquire()

        repository.setStatus(next, DownloadStatus.Downloading)

        try {
            val result = Downloader.fetch(next) // returns Result<DownloadEntry>

            // Success path (Result always success in current implementation)
            repository.setStatus(result)

            println(result)

        } catch (e: Exception) {
            repository.setStatus(next, DownloadStatus.Failed)
            println("Failed ${next.url}: ${e.message}")
        } finally {
            semaphore.release()
        }
    }
}
