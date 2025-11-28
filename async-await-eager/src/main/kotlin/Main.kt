import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import learn.BatchSize
import learn.DownloadEntry
import learn.DownloadStatus
import learn.Downloader
import learn.Repository
import learn.logTime


fun main() = runBlocking {
    val repository = Repository.create()
    val remoteDataSource = Downloader
    val batchSize = BatchSize

    logTime("Overall") {
        val job = launch(Dispatchers.IO + SupervisorJob()) {
            orchestrateDownloads(batchSize, repository, remoteDataSource)
        }
        job.join() // Wait for all downloads to finish
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun orchestrateDownloads(
    batchSize: Int,
    repository: Repository,
    downloader: Downloader
) {
    supervisorScope {
        val deferredList = startInitialBatch(batchSize, repository, downloader)

        while (deferredList.isNotEmpty() || repository.pendingCount() > 0) {

            // Consume one finished (or wait for first to finish)
            val completed = deferredList.awaitAny()

            val result = runCatching { completed.getCompleted() }
            repository.setStatus(result)
            deferredList.remove(completed) // 9

            // Schedule next pending if any
            val next = repository.getNextPendingEntry()
            if (next != null) {
                deferredList.add(
                    asyncDownload(next, downloader, repository)
                )
            }
        }
    }
}

private fun CoroutineScope.startInitialBatch(
    batchSize: Int,
    repository: Repository,
    downloader: Downloader
): MutableList<Deferred<DownloadEntry>> {

    val initial = repository.getTopPendingEntries(batchSize)
    val list = mutableListOf<Deferred<DownloadEntry>>()

    initial.forEach { entry ->
        list += asyncDownload(entry, downloader, repository)
    }

    return list
}

suspend fun <T> Collection<Deferred<T>>.awaitAny(): Deferred<T> =
    select {
        forEach { d ->
            d.onAwait { d }
        }
    }

private fun CoroutineScope.asyncDownload(
    entry: DownloadEntry,
    downloader: Downloader,
    repository: Repository
): Deferred<DownloadEntry> {
    repository.setStatus(entry, DownloadStatus.Downloading) // mark eagerly

    return async(Dispatchers.IO) {
        downloader.fetch(entry).also { println(it) }
        entry
    }
}
