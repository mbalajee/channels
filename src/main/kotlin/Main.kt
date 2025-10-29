package com.example.channel

import com.example.channel.data.DownloadRepository
import com.example.channel.data.RemoteDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope

fun main() = runBlocking {
    val repository = DownloadRepository.demo()
    val remoteDataSource = RemoteDataSource()

    val batchSize = 20
    var batchIndex = 0
    val overallStart = System.nanoTime()

    while (repository.size() > 0) {
        batchIndex++
        val currentBatch = repository.getTopFilesToDownload(batchSize)
        if (currentBatch.isEmpty()) break

        println("\nBatch #$batchIndex -> ${currentBatch.size} files: $currentBatch")
        val batchStart = System.nanoTime()

        supervisorScope {
            currentBatch.map { url ->
                async {
                    cacheFile(url, remoteDataSource)
                    onCached(url, repository)
                }
            }.awaitAll()
        }
        val batchDurationMs = (System.nanoTime() - batchStart) / 1_000_000
        println("Batch #$batchIndex completed in ${batchDurationMs}ms")
    }

    val totalDurationMs = (System.nanoTime() - overallStart) / 1_000_000
    println("\nAll batches processed. Total time: ${totalDurationMs}ms")
}

private suspend fun cacheFile(url: String, remoteDataSource: RemoteDataSource) {
    try {
        val data = remoteDataSource.fetch(url)
        println("Cached $data")
    } catch (e: Exception) {
        println(e.message)
    }
}

private fun onCached(url: String, repository: DownloadRepository) {
    repository.remove(url)
}
