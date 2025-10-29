package com.example.channel.data

class DownloadRepository(initialUrls: List<String>) {

    private val _urls = initialUrls.toMutableList()

    fun getTopFilesToDownload(count: Int): List<String> = _urls.take(count)

    fun remove(url: String) { _urls.remove(url) }

    fun size(): Int = _urls.size

    companion object {
        fun demo(): DownloadRepository {
            val urls = mutableListOf<String>()
            repeat(100) { i ->
                urls += "https://example.com/file${i + 1}.json"
            }
            return DownloadRepository(urls)
        }
    }
}
