package com.antoniegil.astronia.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL

sealed class PingResult {
    data class Success(val latency: Long) : PingResult()
    data class Error(val reason: String) : PingResult()
    object Timeout : PingResult()
}

class PingManager(
    private val maxConcurrent: Int = 5,
    private val maxQueueSize: Int = 100,
    private val requestTimeoutMs: Long = 5000L
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val requestChannel = Channel<PingRequest>(maxQueueSize)
    private val _pingResults = MutableStateFlow<Map<String, PingResult>>(emptyMap())
    val pingResults: StateFlow<Map<String, PingResult>> = _pingResults.asStateFlow()
    
    private val activeRequests = mutableMapOf<String, Job>()
    
    private data class PingRequest(
        val url: String,
        val onResult: (PingResult) -> Unit,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    init {
        repeat(maxConcurrent) {
            scope.launch {
                for (request in requestChannel) {
                    if (System.currentTimeMillis() - request.timestamp > requestTimeoutMs) {
                        request.onResult(PingResult.Timeout)
                        continue
                    }
                    
                    val job = launch {
                        val result = measurePing(request.url)
                        _pingResults.value = _pingResults.value + (request.url to result)
                        request.onResult(result)
                    }
                    
                    activeRequests[request.url] = job
                    
                    try {
                        withTimeout(requestTimeoutMs) {
                            job.join()
                        }
                    } catch (e: TimeoutCancellationException) {
                        job.cancel()
                        val timeoutResult = PingResult.Timeout
                        _pingResults.value = _pingResults.value + (request.url to timeoutResult)
                        request.onResult(timeoutResult)
                        ErrorHandler.logWarning("PingManager", "Ping timeout for ${request.url}")
                    } finally {
                        activeRequests.remove(request.url)
                    }
                }
            }
        }
    }
    
    fun requestPing(url: String, onResult: (PingResult) -> Unit = {}) {
        if (_pingResults.value.containsKey(url)) {
            onResult(_pingResults.value[url] ?: PingResult.Error("Unknown error"))
            return
        }
        
        if (activeRequests.containsKey(url)) {
            return
        }
        
        scope.launch {
            try {
                requestChannel.send(PingRequest(url, onResult))
            } catch (e: Exception) {
                ErrorHandler.logError("PingManager", "Failed to queue ping request for $url", e)
                onResult(PingResult.Error("Queue full"))
            }
        }
    }
    
    fun cancelPing(url: String) {
        activeRequests[url]?.cancel()
        activeRequests.remove(url)
    }
    
    fun cancelAll() {
        activeRequests.values.forEach { it.cancel() }
        activeRequests.clear()
    }
    
    private suspend fun measurePing(url: String): PingResult = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = PlayerConstants.PING_TIMEOUT_MS
            connection.readTimeout = PlayerConstants.PING_TIMEOUT_MS
            connection.setRequestProperty("Range", "bytes=0-8192")
            connection.connect()
            
            val inputStream = connection.inputStream
            val buffer = ByteArray(1024)
            var firstByteReceived = false
            var firstByteLatency = 0L
            
            while (inputStream.read(buffer) != -1) {
                if (!firstByteReceived) {
                    firstByteLatency = System.currentTimeMillis() - start
                    firstByteReceived = true
                    break
                }
            }
            
            inputStream.close()
            connection.disconnect()
            
            if (firstByteLatency >= PlayerConstants.PING_TIMEOUT_MS) {
                PingResult.Timeout
            } else {
                PingResult.Success(firstByteLatency)
            }
        } catch (e: java.net.SocketTimeoutException) {
            ErrorHandler.logWarning("PingManager", "Socket timeout for $url")
            PingResult.Timeout
        } catch (e: Exception) {
            ErrorHandler.logError("PingManager", "Network error for $url", e)
            PingResult.Error(e.message ?: "Network error")
        }
    }
    
    fun clearCache() {
        cancelAll()
        _pingResults.value = emptyMap()
    }
    
    fun shutdown() {
        cancelAll()
        requestChannel.close()
        scope.cancel()
    }
}
