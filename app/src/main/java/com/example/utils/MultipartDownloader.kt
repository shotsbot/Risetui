package com.example.utils

import android.os.Environment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.UUID

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

class MultipartDownloader(private val folder: File) {
    private val client = OkHttpClient()
    
    var speedLimitKBs: Int = 0

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    fun startDownload(url: String, fileName: String, parts: Int = 4) {
        val task = DownloadTask(url = url, fileName = fileName, status = DownloadStatus.DOWNLOADING)
        _tasks.value = _tasks.value + task
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = Request.Builder().url(url).head().build()
                val resp = client.newCall(req).execute()
                val contentLength = resp.header("Content-Length")?.toLongOrNull() ?: 0L
                resp.close()

                if (contentLength <= 0) {
                    standardDownload(task.id, url, fileName)
                    return@launch
                }

                updateTask(task.id) { it.copy(totalBytes = contentLength) }

                val partSize = contentLength / parts
                val deferredParts = (0 until parts).map { i ->
                    async {
                        val start = i * partSize
                        val end = if (i == parts - 1) contentLength - 1 else start + partSize - 1
                        downloadPart(task.id, url, start, end, i)
                    }
                }
                
                val partFiles = deferredParts.awaitAll()
                
                // merge
                val finalFile = File(folder, fileName)
                finalFile.outputStream().use { out ->
                    for (partFile in partFiles) {
                        partFile.inputStream().use { input -> input.copyTo(out) }
                        partFile.delete()
                    }
                }
                
                updateTask(task.id) { it.copy(status = DownloadStatus.COMPLETED, progress = 1f, downloadedBytes = contentLength) }

            } catch (e: Exception) {
                e.printStackTrace()
                updateTask(task.id) { it.copy(status = DownloadStatus.FAILED) }
            }
        }
    }

    private fun downloadPart(taskId: String, url: String, start: Long, end: Long, partIndex: Int): File {
        val req = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .build()
        
        val resp = client.newCall(req).execute()
        val partFile = File(folder, "part_${taskId}_$partIndex")
        
        val body = resp.body ?: throw Exception("Empty body")
        val inStream = body.byteStream()
        val outStream = partFile.outputStream()
        
        val buffer = ByteArray(8192)
        var bytesRead: Int
        
        while (inStream.read(buffer).also { bytesRead = it } != -1) {
            outStream.write(buffer, 0, bytesRead)
            
            // update global progress safely
            updateProgress(taskId, bytesRead.toLong())
            
            val limit = speedLimitKBs
            if (limit > 0) {
                // Approximate speed limit per part. Divide by 4 since we use 4 parts.
                val partLimit = (limit / 4).coerceAtLeast(1)
                val sleepTime = (bytesRead * 1000L) / (partLimit * 1024L)
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime)
                }
            }
        }
        
        outStream.close()
        inStream.close()
        
        return partFile
    }

    private fun standardDownload(taskId: String, url: String, fileName: String) {
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        val finalFile = File(folder, fileName)
        
        val body = resp.body ?: throw Exception("Empty body")
        val inStream = body.byteStream()
        val outStream = finalFile.outputStream()
        
        val buffer = ByteArray(8192)
        var bytesRead: Int
        
        while (inStream.read(buffer).also { bytesRead = it } != -1) {
            outStream.write(buffer, 0, bytesRead)
            
            val limit = speedLimitKBs
            if (limit > 0) {
                val sleepTime = (bytesRead * 1000L) / (limit * 1024L)
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime)
                }
            }
        }
        
        outStream.close()
        inStream.close()
        updateTask(taskId) { it.copy(status = DownloadStatus.COMPLETED, progress = 1f) }
    }

    @Synchronized
    private fun updateProgress(taskId: String, newBytes: Long) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        val newDownloaded = task.downloadedBytes + newBytes
        val progress = if (task.totalBytes > 0) newDownloaded.toFloat() / task.totalBytes else 0f
        updateTask(taskId) { it.copy(downloadedBytes = newDownloaded, progress = progress) }
    }

    private fun updateTask(taskId: String, update: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map { if (it.id == taskId) update(it) else it }
    }
}
