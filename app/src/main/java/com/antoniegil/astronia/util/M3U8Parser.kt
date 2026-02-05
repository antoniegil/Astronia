package com.antoniegil.astronia.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

data class M3U8Channel(
    val id: String,
    val name: String,
    val url: String,
    val group: String = ""
)

object M3U8Parser {
    
    suspend fun parseM3U8(content: String): Result<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        try {
            val channels = mutableListOf<M3U8Channel>()
            val lines = content.lineSequence()
            
            var currentName = ""
            var currentGroup = ""
            var currentId = ""
            
            for (line in lines) {
                val trimmedLine = line.trim()
                
                if (trimmedLine.isEmpty()) continue
                
                if (trimmedLine.startsWith("#EXTINF:")) {
                    currentName = extractChannelName(trimmedLine)
                    currentGroup = extractGroup(trimmedLine)
                    currentId = extractTvgId(trimmedLine)
                } else if (!trimmedLine.startsWith("#")) {
                    if (trimmedLine.startsWith("http") || trimmedLine.startsWith("rtmp") || 
                        trimmedLine.startsWith("rtsp") || trimmedLine.startsWith("udp")) {
                        val name = if (currentName.isNotEmpty()) currentName else "Channel ${channels.size + 1}"
                        val finalId = "${trimmedLine.hashCode()}_${channels.size}"
                        
                        channels.add(
                            M3U8Channel(
                                id = finalId,
                                name = name,
                                url = trimmedLine,
                                group = currentGroup
                            )
                        )
                        
                        if (channels.size >= PlayerConstants.MAX_CHANNEL_DISPLAY) {
                            break
                        }
                    }
                    currentName = ""
                    currentGroup = ""
                    currentId = ""
                }
            }
            
            Result.Success(channels)
        } catch (e: Exception) {
            Result.Error(e, "Failed to parse M3U8 content")
        }
    }
    
    suspend fun parseM3U8FromUrl(url: String): Result<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = PlayerConstants.M3U8_CONNECTION_TIMEOUT_MS
            connection.readTimeout = PlayerConstants.M3U8_READ_TIMEOUT_MS
            
            val contentLength = connection.contentLength
            if (contentLength > PlayerConstants.M3U8_MAX_SIZE_BYTES) {
                return@withContext Result.Error(
                    Exception("Content too large"), 
                    "M3U8 file exceeds size limit"
                )
            }
            
            val channels = mutableListOf<M3U8Channel>()
            var currentName = ""
            var currentGroup = ""
            var currentId = ""
            var totalBytesRead = 0
            
            connection.getInputStream().bufferedReader().use { reader ->
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
                        currentId = extractTvgId(trimmedLine)
                    } else if (!trimmedLine.startsWith("#")) {
                        if (trimmedLine.startsWith("http") || trimmedLine.startsWith("rtmp") || 
                            trimmedLine.startsWith("rtsp") || trimmedLine.startsWith("udp")) {
                            val name = if (currentName.isNotEmpty()) currentName else "Channel ${channels.size + 1}"
                            val finalId = "${trimmedLine.hashCode()}_${channels.size}"
                            
                            channels.add(
                                M3U8Channel(
                                    id = finalId,
                                    name = name,
                                    url = trimmedLine,
                                    group = currentGroup
                                )
                            )
                            
                            if (channels.size >= PlayerConstants.MAX_CHANNEL_DISPLAY) {
                                return@forEach
                            }
                        }
                        currentName = ""
                        currentGroup = ""
                        currentId = ""
                    }
                }
            }
            
            Result.Success(channels)
        } catch (e: Exception) {
            Result.Error(e, "Failed to fetch M3U8 from URL: ${e.message}")
        }
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
    
    private fun extractTvgId(line: String): String {
        val tvgIdMatch = Regex("""tvg-id="([^"]+)"""").find(line)
        return tvgIdMatch?.groupValues?.get(1) ?: ""
    }

    private fun extractGroup(line: String): String {
        val groupMatch = Regex("""group-title="([^"]+)"""").find(line)
        return groupMatch?.groupValues?.get(1) ?: ""
    }
}
