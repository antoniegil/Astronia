package com.antoniegil.astronia.player

import android.content.Context
import android.net.Uri
import com.antoniegil.astronia.util.ErrorHandler
import com.antoniegil.astronia.util.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

object UrlInterceptor {
    
    suspend fun fixMalformedM3u8(context: Context, url: String): String = withContext(Dispatchers.IO) {
        try {
            val client = NetworkUtils.createHttpClient(3000, 5000, true)
            var currentUrl = url
            var depth = 0
            val maxDepth = 5
            
            while (depth < maxDepth) {
                val request = Request.Builder().url(currentUrl).build()
                val response = client.newCall(request).execute()
                val finalUrl = response.request.url.toString()
                val content = response.body.string()
                
                if (!content.trim().startsWith("#EXTM3U")) {
                    return@withContext finalUrl
                }
                
                val isMasterPlaylist = content.contains("#EXT-X-STREAM-INF")
                val isMediaPlaylist = content.contains("#EXTINF")
                
                if (isMediaPlaylist) {
                    return@withContext finalUrl
                }
                
                if (isMasterPlaylist) {
                    val lines = content.lines()
                    val variantUrl = lines.firstOrNull { line ->
                        line.isNotBlank() && !line.startsWith("#")
                    }
                    
                    if (variantUrl != null) {
                        val uri = Uri.parse(finalUrl)
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
                
                return@withContext finalUrl
            }
            
            url
        } catch (_: Exception) {
            url
        }
    }
}
