package com.example.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus = _downloadStatus.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    suspend fun downloadModel(
        context: Context,
        modelUrl: String,
        outputFileName: String,
        authToken: String? = null
    ): File? = withContext(Dispatchers.IO) {
        if (_isDownloading.value) return@withContext null
        _isDownloading.value = true
        _downloadProgress.value = 0f
        _downloadStatus.value = "Connecting to server..."

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = modelUrl
            var redirectCount = 0
            val maxRedirects = 10
            
            while (redirectCount < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = false // Manually handle to preserve auth headers if needed

                // Set a modern web browser User-Agent to prevent 403 Forbidden errors from HuggingFace
                connection.setRequestProperty(
                    "User-Agent", 
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )

                // Only send HuggingFace token to huggingface.co domain to prevent Auth Header disclosure and signature rejection by CDNs (AWS S3, Cloudflare)
                if (!authToken.isNullOrBlank() && currentUrl.contains("huggingface.co")) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl != null) {
                        currentUrl = newUrl
                        redirectCount++
                    } else {
                        throw Exception("Redirect without location header.")
                    }
                } else if (responseCode in 200..299) {
                    break
                } else {
                    throw Exception("Server returned HTTP $responseCode: ${connection.responseMessage ?: "Error"}")
                }
            }

            if (redirectCount >= maxRedirects) {
                throw Exception("Too many redirects.")
            }

            val fileLength = connection!!.contentLengthLong
            val destinationFile = File(context.filesDir, outputFileName)
            
            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val data = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    var lastUpdate = 0L

                    while (input.read(data).also { count = it } != -1) {
                        if (!_isDownloading.value) {
                            throw Exception("Download cancelled by user.")
                        }
                        total += count
                        output.write(data, 0, count)

                        if (fileLength > 0) {
                            val progress = total.toFloat() / fileLength
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdate > 150 || progress == 1.0f) {
                                _downloadProgress.value = progress
                                val downloadedMb = String.format("%.1f", total.toFloat() / (1024 * 1024))
                                val totalMb = String.format("%.1f", fileLength.toFloat() / (1024 * 1024))
                                _downloadStatus.value = "Downloading: $downloadedMb MB / $totalMb MB"
                                lastUpdate = currentTime
                            }
                        } else {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdate > 150) {
                                val downloadedMb = String.format("%.1f", total.toFloat() / (1024 * 1024))
                                _downloadStatus.value = "Downloading: $downloadedMb MB"
                                lastUpdate = currentTime
                            }
                        }
                    }
                }
            }

            _downloadStatus.value = "Success! Saved as: $outputFileName"
            _downloadProgress.value = 1.0f
            LocalGemmaClient.scanModels(context)
            destinationFile
        } catch (e: Exception) {
            e.printStackTrace()
            _downloadStatus.value = "Error: ${e.localizedMessage ?: e.message}"
            _downloadProgress.value = null
            null
        } finally {
            connection?.disconnect()
            _isDownloading.value = false
        }
    }

    fun resetState() {
        _isDownloading.value = false
        _downloadProgress.value = null
        _downloadStatus.value = null
    }

    fun cancelDownload() {
        _isDownloading.value = false
        _downloadProgress.value = null
        _downloadStatus.value = "Download cancelled"
    }
}
