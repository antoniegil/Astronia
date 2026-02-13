package com.antoniegil.astronia.player

import android.content.Context
import androidx.core.net.toUri
import com.antoniegil.astronia.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object UrlInterceptor {
    
    class NetworkBlockedException(message: String) : Exception(message)
    
    suspend fun fixMalformedM3u8(context: Context, url: String): String = withContext(Dispatchers.IO) {
        try {
            val client = NetworkUtils.createHttpClient(3000, 5000, true)
            var currentUrl = url
            var depth = 0
            val maxDepth = 5
            
            while (depth < maxDepth) {
                val request = Request.Builder().url(currentUrl).build()
                val response = client.newCall(request).execute()
                
                val httpCode = response.code
                if (httpCode in listOf(403, 451, 503)) {
                    throw NetworkBlockedException("HTTP $httpCode: Network blocked or restricted")
                }
                
                val content = response.body.string()
                
                if (!content.trim().startsWith("#EXTM3U")) {
                    val contentLower = content.trim().lowercase()
                    if (contentLower.startsWith("<!doctype") || contentLower.startsWith("<html")) {
                        throw NetworkBlockedException("Network blocked or restricted (HTML response)")
                    }
                    return@withContext currentUrl
                }
                
                val isMasterPlaylist = content.contains("#EXT-X-STREAM-INF")
                val isMediaPlaylist = content.contains("#EXTINF")
                
                if (isMediaPlaylist) {
                    return@withContext currentUrl
                }
                
                if (isMasterPlaylist) {
                    val lines = content.lines()
                    val variantUrl = lines.firstOrNull { line ->
                        line.isNotBlank() && !line.startsWith("#")
                    }
                    
                    if (variantUrl != null) {
                        val uri = currentUrl.toUri()
                        val baseUrl = "${uri.scheme}://${uri.host}${if (uri.port == -1) "" else ":${uri.port}"}${uri.path?.substringBeforeLast("/") ?: ""}"
                        
                        currentUrl = if (variantUrl.startsWith("http")) {
                            variantUrl
                        } else {
                            "$baseUrl/${variantUrl.trimStart('/')}"
                        }
                        
                        depth++
                        continue
                    }
                }
                
                return@withContext currentUrl
            }
            
            url
        } catch (e: NetworkBlockedException) {
            throw e
        } catch (_: Exception) {
            url
        }
    }
}
