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
    val downloadedBytes: Long = 0,
    val parts: Int = 4
)

enum class DownloadStatus {
    PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
}

class MultipartDownloader(private val folder: File) {
    private val client = OkHttpClient()
    
    var speedLimitKBs: Int = 0

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val jobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    fun startDownload(url: String, fileName: String, parts: Int = 4) {
        val task = DownloadTask(url = url, fileName = fileName, status = DownloadStatus.PENDING, parts = parts)
        _tasks.value = _tasks.value + task
        resumeDownload(task.id)
    }

    fun pauseDownload(taskId: String) {
        jobs[taskId]?.cancel()
        jobs.remove(taskId)
        updateTask(taskId) { it.copy(status = DownloadStatus.PAUSED) }
    }

    fun resumeDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.status == DownloadStatus.COMPLETED) return
        
        updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }
        
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                var contentLength = task.totalBytes
                if (contentLength <= 0) {
                    val req = Request.Builder().url(task.url).head().build()
                    val resp = client.newCall(req).execute()
                    contentLength = resp.header("Content-Length")?.toLongOrNull() ?: 0L
                    resp.close()
                }

                if (contentLength <= 0) {
                    standardDownload(taskId, task.url, task.fileName)
                    return@launch
                }

                updateTask(taskId) { it.copy(totalBytes = contentLength) }

                val partSize = contentLength / task.parts
                val deferredParts = (0 until task.parts).map { i ->
                    async {
                        val start = i * partSize
                        val end = if (i == task.parts - 1) contentLength - 1 else start + partSize - 1
                        downloadPart(taskId, task.url, start, end, i)
                    }
                }
                
                val partFiles = deferredParts.awaitAll()
                
                // merge
                val finalFile = File(folder, task.fileName)
                finalFile.outputStream().use { out ->
                    for (partFile in partFiles) {
                        partFile.inputStream().use { input -> input.copyTo(out) }
                        partFile.delete()
                    }
                }
                
                updateTask(taskId) { it.copy(status = DownloadStatus.COMPLETED, progress = 1f, downloadedBytes = contentLength) }

            } catch (e: CancellationException) {
                // Was paused
            } catch (e: Exception) {
                e.printStackTrace()
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED) }
            } finally {
                jobs.remove(taskId)
            }
        }
        jobs[taskId] = job
    }

    private suspend fun downloadPart(taskId: String, url: String, start: Long, end: Long, partIndex: Int): File {
        val partFile = File(folder, "part_${taskId}_$partIndex")
        val existingLen = if (partFile.exists()) partFile.length() else 0L
        
        val actualStart = start + existingLen
        if (actualStart > end) return partFile
        
        val req = Request.Builder()
            .url(url)
            .header("Range", "bytes=$actualStart-$end")
            .build()
        
        val resp = client.newCall(req).execute()
        
        val body = resp.body ?: throw Exception("Empty body")
        val inStream = body.byteStream()
        val outStream = java.io.FileOutputStream(partFile, true)
        
        val buffer = ByteArray(8192)
        var bytesRead: Int

        
        while (inStream.read(buffer).also { bytesRead = it } != -1) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
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

    private suspend fun standardDownload(taskId: String, url: String, fileName: String) {
        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        val finalFile = File(folder, fileName)
        
        val body = resp.body ?: throw Exception("Empty body")
        val inStream = body.byteStream()
        val outStream = finalFile.outputStream()
        
        val buffer = ByteArray(8192)
        var bytesRead: Int
        
        while (inStream.read(buffer).also { bytesRead = it } != -1) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
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
