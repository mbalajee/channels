import learn.Repository
import learn.Downloader
import learn.DownloadEntry
import learn.DownloadStatus
import learn.BatchSize
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

fun main() = runBlocking {
    val repository = Repository.create()
    val downloader = Downloader

    val batchSize = BatchSize
    var batchIndex = 0
    val overallStart = System.nanoTime()

    while (repository.pendingCount() > 0) {
        batchIndex++
        val currentBatch = repository.getTopFilesToDownload(batchSize)
        if (currentBatch.isEmpty()) break

        println("\nBatch #$batchIndex -> ${currentBatch.size} files:")
        currentBatch.forEach { println("  ${it.url} (size=${it.size}ms)") }
        val batchStart = System.nanoTime()

        supervisorScope {
            currentBatch.map { entry ->
                async {
                    cacheFile(entry, downloader, repository)
                }
            }.awaitAll()
        }
        val batchDurationMs = (System.nanoTime() - batchStart) / 1_000_000
        println("Batch #$batchIndex completed in ${batchDurationMs}ms")
    }

    val totalDurationMs = (System.nanoTime() - overallStart) / 1_000_000
    println("\nAll batches processed. Total time: ${totalDurationMs}ms")
}

private suspend fun cacheFile(entry: DownloadEntry, downloader: Downloader, repository: Repository) {
    repository.setStatus(entry, DownloadStatus.Downloading)
    val result = downloader.fetch(entry)
    result.onSuccess {
        repository.setStatus(entry, DownloadStatus.Completed)
        println(result)
    }.onFailure { e ->
        repository.setStatus(entry, DownloadStatus.Failed)
        println("Failed: ${entry.url} -> ${e.message}")
    }
}
