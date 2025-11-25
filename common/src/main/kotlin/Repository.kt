package learn

class Repository(initialUrls: List<DownloadEntry>) {

    private val downloadEntries = initialUrls.toMutableList()

    fun getTopPendingEntries(count: Int): List<DownloadEntry> {
        val pendingEntries = downloadEntries.filter { it.status == DownloadStatus.Pending }
        return pendingEntries.take(count)
    }

    fun getNextPendingEntry(): DownloadEntry? {
        return downloadEntries.firstOrNull { it.status == DownloadStatus.Pending }
    }

    fun removeEntry(entry: DownloadEntry) {
        downloadEntries.remove(entry)
    }

    fun setStatus(entry: DownloadEntry, status: DownloadStatus) {
        val index = downloadEntries.indexOf(entry)
        if (index != -1) {
            downloadEntries[index].status = status
        }
    }

    fun setStatus(entry: Result<DownloadEntry>) {
        entry.onSuccess {
            it.status = DownloadStatus.Completed
        }.onFailure {
            entry.getOrNull()?.status = DownloadStatus.Failed
        }
    }

    fun pendingCount(): Int {
        return downloadEntries.count { it.status == DownloadStatus.Pending }
    }

    fun size(): Int = downloadEntries.size

    companion object {
        fun create(): Repository {
            val downloadEntries = mutableListOf<DownloadEntry>()
            repeat(50) { i ->
                downloadEntries += DownloadEntry("https://example.com/file${i + 1}.json")
            }
            return Repository(downloadEntries)
        }
    }
}

class DownloadEntry(
    val url: String,
    var status: DownloadStatus = DownloadStatus.Pending //
)

enum class DownloadStatus {
    Pending,
    Downloading,
    Completed,
    Failed
}
