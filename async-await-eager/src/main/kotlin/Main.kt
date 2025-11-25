import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import learn.BatchSize
import learn.DownloadEntry
import learn.DownloadStatus
import learn.Downloader
import learn.Repository
import learn.logTime

var count = 0

fun main() = runBlocking {
    val repository = Repository.create()
    val remoteDataSource = Downloader
    val batchSize = BatchSize

    logTime("Overall") {
        val job = launch(Dispatchers.IO + SupervisorJob()) {
            orchestrateDownloads(batchSize, repository, remoteDataSource)
        }
        job.join() // Wait for all downloads to finish
        println("Downloads $count")
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
            deferredList.remove(completed)

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

/**
 * Batch finish-handling: after one winner, quickly check and drain any other
 * already-completed Deferreds to reduce the number of select rebuilds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun drainCompleted(
    deferreds: MutableList<Deferred<DownloadEntry>>,
    repository: Repository
) {
    val it = deferreds.iterator()
    while (it.hasNext()) {
        val d = it.next()
        if (d.isCompleted) {
            // Update status using Result wrapper; await won't suspend here
            val result: Result<DownloadEntry> = runCatching { d.getCompleted() }
            repository.setStatus(result)
            it.remove()
        }
    }
}

private fun CoroutineScope.asyncDownload(
    entry: DownloadEntry,
    downloader: Downloader,
    repository: Repository
): Deferred<DownloadEntry> {
    repository.setStatus(entry, DownloadStatus.Downloading) // mark eagerly

    return async(Dispatchers.IO) {
        count++
        downloader.fetch(entry).also { println(it) }
        entry
    }
}
