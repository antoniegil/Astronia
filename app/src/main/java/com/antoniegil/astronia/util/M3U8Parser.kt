package com.antoniegil.astronia.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class M3U8Channel(
    val id: String,
    val name: String,
    val url: String,
    val group: String = "",
    val country: String = "",
    val language: String = "",
    val logoUrl: String = ""
)

object M3U8Parser {
    
    private val client by lazy {
        NetworkUtils.createHttpClient(
            connectTimeoutMs = PlayerConstants.M3U8_CONNECTION_TIMEOUT_MS.toLong(),
            readTimeoutMs = PlayerConstants.M3U8_READ_TIMEOUT_MS.toLong(),
            trustAllCerts = true
        )
    }
    
    suspend fun parseM3U8(content: String): Result<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        try {
            val channels = mutableListOf<M3U8Channel>()
            val lines = content.lineSequence()
            
            var currentName = ""
            var currentGroup = ""
            var currentCountry = ""
            var currentLanguage = ""
            var currentLogoUrl = ""
            
            for (line in lines) {
                val trimmedLine = line.trim()
                
                if (trimmedLine.isEmpty()) continue
                
                if (trimmedLine.startsWith("#EXTINF:")) {
                    currentName = extractChannelName(trimmedLine)
                    currentGroup = extractGroup(trimmedLine)
                    currentCountry = extractCountry(trimmedLine)
                    currentLanguage = extractLanguage(trimmedLine)
                    currentLogoUrl = extractLogoUrl(trimmedLine)
                } else if (!trimmedLine.startsWith("#")) {
                    if (trimmedLine.startsWith("http") || trimmedLine.startsWith("rtmp") || 
                        trimmedLine.startsWith("rtsp") || trimmedLine.startsWith("udp")) {
                        val name = currentName.ifEmpty { "Channel ${channels.size + 1}" }
                        val finalId = "${trimmedLine.hashCode()}_${channels.size}"
                        
                        channels.add(
                            M3U8Channel(
                                id = finalId,
                                name = name,
                                url = trimmedLine,
                                group = currentGroup,
                                country = currentCountry,
                                language = currentLanguage,
                                logoUrl = currentLogoUrl
                            )
                        )
                        
                        if (channels.size >= PlayerConstants.MAX_CHANNEL_DISPLAY) {
                            break
                        }
                    }
                    currentName = ""
                    currentGroup = ""
                    currentCountry = ""
                    currentLanguage = ""
                    currentLogoUrl = ""
                }
            }
            
            Result.Success(channels)
        } catch (e: Exception) {
            Result.Error(e, "Failed to parse M3U8 content")
        }
    }
    
    private data class FetchResult(
        val channels: List<M3U8Channel>,
        val finalUrl: String
    )
    
    suspend fun parseM3U8FromUrl(url: String): Result<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        val httpsUrl = NetworkUtils.convertToHttps(url)
        
        try {
            val result = fetchM3U8Channels(httpsUrl)
            if (result.channels.isEmpty()) {
                val singleChannel = listOf(
                    M3U8Channel(
                        id = "${result.finalUrl.hashCode()}_0",
                        name = result.finalUrl.substringAfterLast("/").substringBeforeLast(".").ifEmpty { 
                            result.finalUrl.substringAfterLast("/").substringBefore("?").ifEmpty { "Stream" }
                        },
                        url = result.finalUrl
                    )
                )
                return@withContext Result.Success(singleChannel)
            }
            Result.Success(result.channels)
        } catch (e: Exception) {
            if (httpsUrl != url) {
                try {
                    val result = fetchM3U8Channels(url)
                    if (result.channels.isEmpty()) {
                        val singleChannel = listOf(
                            M3U8Channel(
                                id = "${result.finalUrl.hashCode()}_0",
                                name = result.finalUrl.substringAfterLast("/").substringBeforeLast(".").ifEmpty { 
                                    result.finalUrl.substringAfterLast("/").substringBefore("?").ifEmpty { "Stream" }
                                },
                                url = result.finalUrl
                            )
                        )
                        return@withContext Result.Success(singleChannel)
                    }
                    Result.Success(result.channels)
                } catch (fallbackException: Exception) {
                    Result.Error(fallbackException, "Failed to fetch M3U8 from URL: ${fallbackException.message}")
                }
            } else {
                Result.Error(e, "Failed to fetch M3U8 from URL: ${e.message}")
            }
        }
    }
    
    private fun fetchM3U8Channels(url: String): FetchResult {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }
        
        val finalUrl = response.request.url.toString()
        
        val contentLength = response.body.contentLength()
        if (contentLength > PlayerConstants.M3U8_MAX_SIZE_BYTES) {
            response.close()
            throw Exception("Content too large")
        }
        
        val channels = mutableListOf<M3U8Channel>()
        var currentName = ""
        var currentGroup = ""
        var currentCountry = ""
        var currentLanguage = ""
        var currentLogoUrl = ""
        var totalBytesRead = 0
        
        response.body.byteStream().bufferedReader().use { reader ->
            reader.lineSequence().forEach { line ->
                totalBytesRead += line.length + 1
                
                if (totalBytesRead > PlayerConstants.M3U8_MAX_SIZE_BYTES) {
                    return@forEach
                }
                
                val trimmedLine = line.trim()
                
                if (trimmedLine.isEmpty()) return@forEach
                
                if (trimmedLine.startsWith("#EXTINF:")) {
                    currentName = extractChannelName(trimmedLine)
                    currentGroup = extractGroup(trimmedLine)
                    currentCountry = extractCountry(trimmedLine)
                    currentLanguage = extractLanguage(trimmedLine)
                    currentLogoUrl = extractLogoUrl(trimmedLine)
                } else if (!trimmedLine.startsWith("#")) {
                    if (trimmedLine.startsWith("http") || trimmedLine.startsWith("rtmp") || 
                        trimmedLine.startsWith("rtsp") || trimmedLine.startsWith("udp")) {
                        val name = currentName.ifEmpty { "Channel ${channels.size + 1}" }
                        val finalId = "${trimmedLine.hashCode()}_${channels.size}"
                        
                        channels.add(
                            M3U8Channel(
                                id = finalId,
                                name = name,
                                url = trimmedLine,
                                group = currentGroup,
                                country = currentCountry,
                                language = currentLanguage,
                                logoUrl = currentLogoUrl
                            )
                        )
                        
                        if (channels.size >= PlayerConstants.MAX_CHANNEL_DISPLAY) {
                            return@forEach
                        }
                    }
                    currentName = ""
                    currentGroup = ""
                    currentCountry = ""
                    currentLanguage = ""
                    currentLogoUrl = ""
                }
            }
        }
        
        return FetchResult(channels, finalUrl)
    }
    
    private fun extractChannelName(line: String): String {
        val commaIndex = line.lastIndexOf(',')
        if (commaIndex != -1 && commaIndex < line.length - 1) {
            val name = line.substring(commaIndex + 1).trim()
            if (name.isNotEmpty()) return name
        }
        
        val tvgNameMatch = Regex("""tvg-name="([^"]+)"""").find(line)
        if (tvgNameMatch != null) {
            return tvgNameMatch.groupValues[1]
        }
        
        val tvgIdMatch = Regex("""tvg-id="([^"]+)"""").find(line)
        if (tvgIdMatch != null) {
            return tvgIdMatch.groupValues[1]
        }
        
        return ""
    }
    
    private fun extractGroup(line: String): String {
        val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
        return groupMatch?.groupValues?.get(1) ?: ""
    }
    
    private fun extractCountry(line: String): String {
        val countryMatch = Regex("""tvg-country="([^"]+)"""").find(line)
        return countryMatch?.groupValues?.get(1) ?: ""
    }
    
    private fun extractLanguage(line: String): String {
        val languageMatch = Regex("""tvg-language="([^"]+)"""").find(line)
        return languageMatch?.groupValues?.get(1) ?: ""
    }
    
    private fun extractLogoUrl(line: String): String {
        val logoMatch = Regex("""tvg-logo="([^"]+)"""").find(line)
        return logoMatch?.groupValues?.get(1) ?: ""
    }
}
