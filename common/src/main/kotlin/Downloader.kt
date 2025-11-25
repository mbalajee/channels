package learn

import kotlinx.coroutines.delay

object Downloader {

    suspend fun fetch(entry: DownloadEntry): Result<DownloadEntry> {
        val wait = entry.size.toLong()

        delay(wait)

        return Result.success(entry)
    }
}
