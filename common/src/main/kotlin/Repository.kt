package learn

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class Repository(initialUrls: List<DownloadEntry>) {

    private val downloadEntries = initialUrls.toMutableList()

    fun getTopPendingEntries(count: Int): List<DownloadEntry> {
        val pendingEntries = downloadEntries.filter { it.status == DownloadStatus.Pending }
        return pendingEntries.take(count)
    }

    fun getNextPendingEntry(): DownloadEntry? {
        return downloadEntries.firstOrNull { it.status == DownloadStatus.Pending }
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

    fun getTopFilesToDownload(count: Int): List<DownloadEntry> = getTopPendingEntries(count)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun create(): Repository {
            val stream = Repository::class.java.classLoader.getResourceAsStream("downloads.json")
                ?: error("downloads.json resource not found")
            val text = stream.bufferedReader().use { it.readText() }
            val downloadEntries = json.decodeFromString<List<DownloadEntry>>(text).toMutableList()
            return Repository(downloadEntries)
        }
    }
}

@Serializable
data class DownloadEntry(
    val url: String,
    var status: DownloadStatus = DownloadStatus.Pending,
    val size: Int // delay in ms
)

enum class DownloadStatus {
    Pending,
    Downloading,
    Completed,
    Failed
}
