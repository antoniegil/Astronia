package com.antoniegil.astronia.util

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    
    private val client: OkHttpClient by lazy {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N) {
            @Suppress("CustomX509TrustManager")
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Suppress("TrustAllX509TrustManager")
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                @Suppress("TrustAllX509TrustManager")
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, trustAllCerts, SecureRandom())
            }
            
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(PlayerConstants.M3U8_CONNECTION_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(PlayerConstants.M3U8_READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()
        } else {
            OkHttpClient.Builder()
                .connectTimeout(PlayerConstants.M3U8_CONNECTION_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(PlayerConstants.M3U8_READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()
        }
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
    
    suspend fun parseM3U8FromUrl(url: String): Result<List<M3U8Channel>> = withContext(Dispatchers.IO) {
        try {
            Log.d("M3U8Parser", "Fetching M3U8 from: $url")
            Log.d("M3U8Parser", "OkHttp version: ${OkHttpClient::class.java.`package`?.implementationVersion}")
            
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            Log.d("M3U8Parser", "Response code: ${response.code}")
            
            if (!response.isSuccessful) {
                return@withContext Result.Error(
                    Exception("HTTP ${response.code}"),
                    "Failed to fetch M3U8"
                )
            }
            
            val contentLength = response.body.contentLength()
            if (contentLength > PlayerConstants.M3U8_MAX_SIZE_BYTES) {
                response.close()
                return@withContext Result.Error(
                    Exception("Content too large"),
                    "M3U8 file exceeds size limit"
                )
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
